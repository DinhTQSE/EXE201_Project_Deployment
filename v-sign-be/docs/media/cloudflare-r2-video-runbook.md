# V-Sign Video Hosting Runbook: Cloudflare R2

## Kiến trúc chốt cho MVP

```txt
Local MP4 dataset
  -> Cloudflare R2 bucket
  -> R2 custom domain hoặc r2.dev development URL
  -> PostgreSQL video_url stores public media URL
  -> FE renders videoUrl from BE API
```

Khuyến nghị:

- MVP/demo: dùng R2 public bucket qua custom domain nếu có domain, hoặc `r2.dev` nếu chỉ demo nhanh.
- Production/premium: dùng R2 private + Cloudflare Worker/BE signed URL sau khi cần enforce subscription thật sự.
- DB vẫn lưu `learning_lessons.video_url`, `dictionary_entries.video_url`, và `practice_items.video_url`.
- FE không cần đổi logic vì đã render `videoUrl` từ backend.

## Vì sao dùng R2

Cloudflare R2 free tier hiện gồm:

- 10 GB-month storage mỗi tháng.
- 1,000,000 Class A operations mỗi tháng.
- 10,000,000 Class B operations mỗi tháng.
- Egress ra Internet miễn phí.

Dataset hiện tại khoảng 2.7 GB nên phù hợp MVP.

Nguồn:

- R2 pricing: https://developers.cloudflare.com/r2/pricing/
- Create buckets: https://developers.cloudflare.com/r2/buckets/create-buckets/
- Upload objects: https://developers.cloudflare.com/r2/objects/upload-objects/
- Public buckets/custom domains: https://developers.cloudflare.com/r2/data-access/public-buckets/

## Option A: Có domain riêng

Ví dụ domain:

```txt
vsign.app
```

Media domain đề xuất:

```txt
media.vsign.app
```

### 1. Thêm domain vào Cloudflare

1. Vào Cloudflare Dashboard.
2. Websites > Add a site.
3. Nhập domain.
4. Chọn Free plan nếu chỉ dùng MVP.
5. Làm theo hướng dẫn đổi nameserver ở nhà cung cấp domain.
6. Chờ status domain Active.

### 2. Tạo R2 bucket

1. Cloudflare Dashboard > R2 Object Storage.
2. Create bucket.
3. Bucket name:

```txt
vsign-learning-videos
```

4. Location/jurisdiction: để mặc định nếu Cloudflare không yêu cầu chọn.
5. Create bucket.

Bucket R2 mặc định không public.

### 3. Upload video

Cloudflare Dashboard chỉ cho upload tối đa 100 files/lần. Dataset hiện có hơn 4,000 files, nên không upload bằng Dashboard.

Ưu tiên dùng AWS CLI với R2 S3-compatible API.

#### 3.1. Tạo R2 API token/access key

1. Cloudflare Dashboard > R2 Object Storage.
2. Chọn `Manage R2 API tokens`.
3. Chọn `Create API token`.
4. Permission:

```txt
Object Read & Write
```

5. Scope:

```txt
Apply to specific bucket: vsign-learning-videos
```

6. TTL:
   - Chọn thời gian đủ upload, ví dụ 1 ngày hoặc 7 ngày.
7. Create token.
8. Copy và lưu tạm:

```txt
Access Key ID
Secret Access Key
Account ID
```

Không commit hoặc gửi các key này qua chat.

#### 3.2. Cấu hình AWS CLI profile riêng cho R2

```powershell
aws configure --profile r2
```

Nhập:

```txt
AWS Access Key ID: <R2 Access Key ID>
AWS Secret Access Key: <R2 Secret Access Key>
Default region name: auto
Default output format: json
```

#### 3.3. Upload bằng script

Từ thư mục gốc project:

```powershell
cd D:\V-sign_EXE101_Project

powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\sync-videos-to-r2.ps1 `
  -AccountId <CLOUDFLARE_ACCOUNT_ID> `
  -BucketName vsign-learning-videos `
  -SourceVideoDir D:\raw_videos\archive\Dataset\Videos `
  -Prefix videos `
  -Profile r2
```

Script sẽ upload tất cả `.mp4` vào:

```txt
videos/<filename>.mp4
```

Ví dụ:

```txt
videos/W00489.mp4
```

Nếu muốn test trước với 10 file:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\sync-videos-to-r2.ps1 `
  -AccountId <CLOUDFLARE_ACCOUNT_ID> `
  -BucketName vsign-learning-videos `
  -SourceVideoDir D:\raw_videos\archive\Dataset\Videos `
  -Prefix videos `
  -Profile r2 `
  -MaxFiles 10
```

Sau khi test URL ổn, chạy lại không có `-MaxFiles` để upload toàn bộ.

#### 3.4. Upload bằng Dashboard chỉ dùng cho test nhỏ

1. Mở bucket `vsign-learning-videos`.
2. Chọn Create folder.
3. Tạo folder:

```txt
videos
```

4. Vào folder `videos`.
5. Chọn Upload.
6. Upload các file `.mp4` từ:

```txt
D:\raw_videos\archive\Dataset\Videos
```

Với MVP, có thể upload toàn bộ 4,362 files vì tổng dung lượng khoảng 2.7 GB.

### 4. Gắn custom domain vào bucket

1. Mở bucket `vsign-learning-videos`.
2. Settings.
3. Custom Domains.
4. Add.
5. Nhập:

```txt
media.vsign.app
```

6. Continue.
7. Cloudflare sẽ tạo DNS record tương ứng.
8. Connect Domain.
9. Chờ status chuyển sang Active.

Sau khi active, URL video sẽ là:

```txt
https://media.vsign.app/videos/W00489.mp4
```

### 5. Caching

Nếu muốn cache tốt hơn cho video:

1. Cloudflare Dashboard > Rules > Cache Rules.
2. Create rule.
3. Rule name:

```txt
Cache R2 video assets
```

4. If:

```txt
Hostname equals media.vsign.app
```

5. Then:

```txt
Cache eligibility: Eligible for cache
Edge TTL: 1 month hoặc 1 year
Browser TTL: Respect origin hoặc 1 month
```

Nếu file video không thay đổi tên sau upload, TTL dài là ổn. Nếu thay nội dung video, nên đổi filename hoặc path version thay vì purge cache liên tục.

### 6. Sinh SQL backfill DB

Từ thư mục gốc project:

```powershell
cd D:\V-sign_EXE101_Project

powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\generate-video-url-backfill-sql.ps1 `
  -MediaBaseUrl https://media.vsign.app `
  -Prefix videos `
  -LabelCsvPath D:\raw_videos\archive\Dataset\Labels\label.csv
```

File output:

```txt
v-sign-be\scripts\media\generated-video-url-backfill.sql
```

Chạy file này trong Supabase SQL Editor.

## Option B: Chưa có domain riêng

Dùng `r2.dev` để demo nhanh.

### 1. Tạo bucket và upload video

Làm giống Option A bước 2-3.

### 2. Bật public development URL

1. Mở bucket.
2. Settings.
3. Public access hoặc Public Development URL.
4. Enable.
5. Copy URL dạng:

```txt
https://pub-xxxxx.r2.dev
```

Cloudflare ghi rõ `r2.dev` dành cho non-production và có rate limit. Dùng cho demo/lớp học/MVP ban đầu thì được.

### 3. Sinh SQL backfill DB

```powershell
cd D:\V-sign_EXE101_Project

powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\generate-video-url-backfill-sql.ps1 `
  -MediaBaseUrl https://pub-xxxxx.r2.dev `
  -Prefix videos `
  -LabelCsvPath D:\raw_videos\archive\Dataset\Labels\label.csv
```

Chạy SQL output trong Supabase SQL Editor.

## Production CORS

Only allow staging and production frontend origins to read videos. Do not add `localhost` to the production bucket CORS rule. If local development needs direct R2 video reads, use a separate dev bucket or temporary local-only rule.

From the project root:

```powershell
cd D:\V-sign_EXE101_Project

powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\configure-r2-cors.ps1 `
  -AccountId <CLOUDFLARE_ACCOUNT_ID> `
  -BucketName vsign-learning-videos `
  -AllowedOrigins https://vsign.vercel.app,https://staging-vsign.vercel.app `
  -Profile r2
```

The script writes `v-sign-be\scripts\media\generated-r2-cors.json` and applies it with:

```txt
AllowedMethods: GET, HEAD
AllowedHeaders: Range, If-None-Match, If-Modified-Since
ExposeHeaders: Accept-Ranges, Content-Length, Content-Range, Content-Type, ETag, Cache-Control
```

## Cache headers

`sync-videos-to-r2.ps1` uploads MP4 files with:

```txt
Cache-Control: public,max-age=31536000,immutable
Content-Type: video/mp4
```

This is correct for versioned/static MP4 filenames. If a video changes, upload it under a new filename or versioned path instead of replacing the object behind the same URL.

## Verify media URLs

Verify generated SQL offline without HTTP calls:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\verify-video-urls.ps1 `
  -SqlPath .\v-sign-be\scripts\media\generated-video-url-backfill.sql `
  -ListOnly
```

Verify a deployed backend API and fail on non-`200/206` responses or non-`video/mp4` content:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\verify-video-urls.ps1 `
  -ApiBaseUrl https://api.vsign.example.com `
  -RequireCacheControl `
  -ReportPath .\v-sign-be\scripts\media\video-url-check-report.csv
```

For local backend testing:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\verify-video-urls.ps1 `
  -ApiBaseUrl http://localhost:8080/V-sign `
  -MaxUrls 20
```

## Verify sau khi backfill

Chạy backend:

```powershell
cd D:\V-sign_EXE101_Project\v-sign-be
mvn.cmd spring-boot:run
```

Kiểm tra API:

```txt
GET http://localhost:8080/api/v1/lessons/lesson-greetings-1
GET http://localhost:8080/api/v1/dictionary?size=10
GET http://localhost:8080/api/v1/learning/practice-items?size=10
```

Kỳ vọng:

- `videoUrl` là `https://media.../videos/...` hoặc `https://pub-...r2.dev/videos/...`.
- Không còn URL S3/CloudFront trong các record mới.
- FE lesson/dictionary modal load được video.

## Mapping video theo vùng miền

Dataset có quy ước hậu tố filename:

```txt
B = Mien Bac
T = Mien Trung
N = Mien Nam
```

Ví dụ:

```txt
D0001B.mp4
D0001T.mp4
D0001N.mp4
```

Script backfill không bỏ qua metadata này. Khi sinh SQL, script:

- Tách `region_code` từ suffix filename.
- Ghi tất cả biến thể vào `dictionary_entry_video_variants`.
- Vẫn cập nhật `dictionary_entries.video_url` bằng một video mặc định để FE hiện tại chạy được.

Mặc định video đại diện dùng thứ tự đầu tiên trong `label.csv`. Nếu muốn ưu tiên một vùng cụ thể cho `dictionary_entries.video_url`, truyền thêm:

```powershell
-DefaultRegionCode BAC
```

Các giá trị hợp lệ:

```txt
FIRST
BAC
TRUNG
NAM
TOAN_QUOC
```

Ví dụ ưu tiên Miền Bắc:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\generate-video-url-backfill-sql.ps1 `
  -MediaBaseUrl https://pub-xxxxx.r2.dev `
  -Prefix videos `
  -LabelCsvPath D:\raw_videos\archive\Dataset\Labels\label.csv `
  -DefaultRegionCode BAC
```

Sau này FE có thể thêm selector `Bac/Trung/Nam` bằng cách đọc bảng biến thể từ BE API mới.

## Khi cần premium/private thật

Public R2 URL không enforce quyền Premium. Ai có link đều có thể mở.

Hướng production:

```txt
FE asks BE for video playback URL
  -> BE checks JWT/subscription
  -> BE returns short-lived signed URL hoặc Worker token URL
  -> FE plays video
```

Không implement phần này cho MVP nếu mục tiêu hiện tại là hoàn thiện sản phẩm demo ổn định trước.
