# V-Sign Video Hosting Runbook: Private S3 + CloudFront OAC

## Kiến trúc chốt

```txt
Local MP4 dataset
  -> private S3 bucket
  -> CloudFront distribution with OAC signing requests to S3
  -> PostgreSQL video_url stores CloudFront URL
  -> FE renders videoUrl from BE API
```

Quy tắc:

- S3 bucket phải bật Block Public Access 100%.
- Không dùng public ACL.
- Không lưu S3 URL vào DB.
- DB chỉ lưu CloudFront URL dạng `https://dxxxxx.cloudfront.net/videos/W00489.mp4`.
- CloudFront đọc S3 bằng Origin Access Control (OAC), `SigningBehavior=always`.

Lưu ý bảo mật: OAC chỉ chặn truy cập trực tiếp vào S3. Nếu CloudFront distribution public thì ai có CloudFront URL vẫn xem được video. Khi cần bảo vệ video premium theo user, phải thêm CloudFront signed URL/signed cookie hoặc BE endpoint cấp URL ngắn hạn.

## Tài liệu AWS chính thức

- CloudFront OAC cho S3 private origin: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html
- S3 Block Public Access: https://docs.aws.amazon.com/AmazonS3/latest/userguide/configuring-block-public-access-bucket.html
- AWS CLI `create-origin-access-control`: https://docs.aws.amazon.com/cli/latest/reference/cloudfront/create-origin-access-control.html
- CloudFront Free Tier FAQ: https://aws.amazon.com/cloudfront/faqs/

AWS FAQ hiện nêu CloudFront Free Tier gồm 1 TB data transfer out, 10,000,000 HTTP/HTTPS requests và 2,000,000 CloudFront Function invocations mỗi tháng. S3 storage, S3 requests, invalidations vượt mức miễn phí, custom domain/Route53 hoặc WAF nếu thêm vẫn có thể phát sinh chi phí.

## Bước 0: Chuẩn bị AWS credential

Không gửi AWS Access Key/Secret Key vào chat hoặc commit vào repo.

Trên máy local:

```powershell
aws configure
aws sts get-caller-identity
```

Credential hiện tại trên máy đã từng báo `InvalidClientTokenId`, nên cần cấu hình lại trước khi deploy.

IAM user/role cần quyền tối thiểu theo nhóm:

- `cloudformation:*` trên stack triển khai.
- `s3:CreateBucket`, `s3:PutBucketPolicy`, `s3:PutPublicAccessBlock`, `s3:PutBucketOwnershipControls`, `s3:PutBucketVersioning`, `s3:PutObject`, `s3:GetObject`, `s3:ListBucket`.
- `cloudfront:CreateDistribution`, `cloudfront:CreateOriginAccessControl`, `cloudfront:GetDistribution`, `cloudfront:UpdateDistribution`, `cloudfront:ListDistributions`.
- `sts:GetCallerIdentity`.

## Bước 1: Deploy private S3 + CloudFront OAC

Từ thư mục gốc project:

```powershell
cd D:\V-sign_EXE101_Project
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\deploy-private-media-stack.ps1 `
  -StackName v-sign-media `
  -BucketName v-sign-media-prod-<ten-ngan-gon-duy-nhat> `
  -Region ap-southeast-1 `
  -PriceClass PriceClass_200
```

Ghi lại output:

```txt
BucketName
DistributionId
CloudFrontDomainName
CloudFrontBaseUrl
```

CloudFront deploy thường mất vài phút để chuyển sang trạng thái sẵn sàng.

### Troubleshooting: CloudFront account verification

Nếu CloudFormation fail với lỗi:

```txt
Your account must be verified before you can add new CloudFront resources.
```

thì template không phải nguyên nhân chính. AWS đang chặn account tạo CloudFront distribution cho đến khi account/billing/identity verification hoàn tất.

Việc cần làm:

1. Vào AWS Support Center: https://console.aws.amazon.com/support/home#/
2. Tạo case Account and billing.
3. Chọn nội dung liên quan Account activation/account verification.
4. Dán nguyên lỗi CloudFront vào case.
5. Kiểm tra thêm Billing > Payment preferences và email/phone verification của account.

Sau khi AWS verify xong, xoá stack rollback cũ rồi deploy lại:

```powershell
aws cloudformation delete-stack `
  --stack-name v-sign-media `
  --region ap-southeast-1

aws cloudformation wait stack-delete-complete `
  --stack-name v-sign-media `
  --region ap-southeast-1
```

Không chuyển S3 sang public để né lỗi này. Kiến trúc media chính thức vẫn là private S3 + CloudFront OAC.

## Cách tạo thủ công trên AWS Console

Các bước dưới đây dùng AWS web UI thay cho CloudFormation/CLI. Nếu account vẫn báo `Your account must be verified before you can add new CloudFront resources`, cách manual cũng sẽ bị chặn ở bước tạo CloudFront distribution. Khi đó phải xử lý account verification trước.

### 1. Tạo S3 bucket private

1. Mở AWS Console > S3 > Buckets > Create bucket.
2. Region: `Asia Pacific (Singapore) ap-southeast-1`.
3. Bucket name: chọn tên duy nhất toàn cầu, ví dụ `v-sign-media-prod-<suffix>`.
4. Object Ownership:
   - Chọn `ACLs disabled`
   - Chọn `Bucket owner enforced`
5. Block Public Access settings:
   - Bật toàn bộ 4 lựa chọn block public access.
   - Không bỏ tick mục nào.
6. Bucket Versioning:
   - Có thể bật `Enable`.
7. Default encryption:
   - Dùng `Amazon S3 managed keys (SSE-S3)` để đơn giản.
8. Create bucket.

### 2. Upload video vào bucket

1. Mở bucket vừa tạo.
2. Chọn Create folder, tạo folder:

```txt
videos
```

3. Vào folder `videos`.
4. Chọn Upload.
5. Add files từ:

```txt
D:\raw_videos\archive\Dataset\Videos
```

6. Upload các file MP4 cần dùng trước, ví dụ:

```txt
W00489.mp4
W03990.mp4
W00110B.mp4
```

7. Không chọn public ACL.

### 3. Tạo CloudFront distribution với S3 origin

1. Mở AWS Console > CloudFront > Distributions > Create distribution.
2. Origin domain:
   - Chọn bucket S3 vừa tạo.
   - Dùng S3 bucket origin, không dùng static website endpoint.
3. Origin path:
   - Để trống.
4. Name:

```txt
v-sign-private-media-origin
```

5. Origin access:
   - Chọn `Origin access control settings (recommended)`.
6. Origin access control:
   - Chọn `Create new OAC`.
   - Name: `v-sign-media-oac`.
   - Signing behavior: `Sign requests (recommended)`.
   - Origin type: `S3`.
   - Create.
7. Viewer protocol policy:
   - Chọn `Redirect HTTP to HTTPS`.
8. Allowed HTTP methods:
   - Chọn `GET, HEAD`.
   - Nếu console yêu cầu OPTIONS/CORS, có thể chọn `GET, HEAD, OPTIONS`.
9. Cache policy:
   - Chọn `CachingOptimized` nếu có.
10. Web Application Firewall:
   - Chọn không bật WAF ở MVP để tránh phát sinh chi phí.
11. Price class:
   - Chọn `Use only North America and Europe` nếu muốn tiết kiệm nhất, hoặc
   - Chọn option có Asia nếu muốn tối ưu tốc độ cho Việt Nam.
12. Alternate domain name/CNAME:
   - Để trống ở MVP.
13. Custom SSL certificate:
   - Để default CloudFront certificate.
14. Create distribution.

Sau khi tạo xong, copy:

```txt
Distribution ID
Distribution domain name
```

Ví dụ domain:

```txt
d123abc456.cloudfront.net
```

### 4. Update S3 bucket policy cho CloudFront OAC

Sau khi tạo distribution, CloudFront console thường hiển thị một đoạn bucket policy cần copy. Nếu có nút `Copy policy`, dùng policy đó.

Nếu cần tự nhập:

1. Mở S3 > bucket > Permissions > Bucket policy > Edit.
2. Dán policy, thay:
   - `<BUCKET_NAME>`
   - `<ACCOUNT_ID>`
   - `<DISTRIBUTION_ID>`

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontServicePrincipalReadOnly",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudfront.amazonaws.com"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::<BUCKET_NAME>/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "arn:aws:cloudfront::<ACCOUNT_ID>:distribution/<DISTRIBUTION_ID>"
        }
      }
    }
  ]
}
```

3. Save changes.

### 5. Kiểm tra URL

Direct S3 URL phải fail:

```txt
https://<BUCKET_NAME>.s3.ap-southeast-1.amazonaws.com/videos/W00489.mp4
```

Kỳ vọng: `AccessDenied`.

CloudFront URL phải chạy:

```txt
https://<DISTRIBUTION_DOMAIN>/videos/W00489.mp4
```

Kỳ vọng: video load được.

### 6. Sinh SQL backfill sau khi có CloudFront domain

Sau khi CloudFront chạy được, dùng script local để sinh SQL DB:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\generate-video-url-backfill-sql.ps1 `
  -CloudFrontDomain https://<DISTRIBUTION_DOMAIN> `
  -Prefix videos `
  -LabelCsvPath D:\raw_videos\archive\Dataset\Labels\label.csv
```

Mở file:

```txt
v-sign-be\scripts\media\generated-video-url-backfill.sql
```

Chạy trong Supabase SQL Editor.

## Bước 2: Upload video lên private S3

Script này đọc danh sách `source_video_file` từ seed migrations `practice_items`, rồi upload đúng các file đó từ dataset local.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\sync-videos-to-s3.ps1 `
  -BucketName <BucketName-tu-output> `
  -SourceVideoDir D:\raw_videos\archive\Dataset\Videos `
  -Prefix videos `
  -Region ap-southeast-1
```

Script không set `public-read`, không tạo ACL public.

## Bước 3: Sinh SQL backfill CloudFront URL

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\v-sign-be\scripts\media\generate-video-url-backfill-sql.ps1 `
  -CloudFrontDomain https://<CloudFrontDomainName> `
  -Prefix videos `
  -LabelCsvPath D:\raw_videos\archive\Dataset\Labels\label.csv
```

File sinh ra:

```txt
v-sign-be\scripts\media\generated-video-url-backfill.sql
```

SQL này làm 3 việc:

- Cập nhật `practice_items.video_url` nếu DB đã có column từ migration V12.
- Cập nhật `learning_lessons.video_url` bằng video đầu tiên thuộc lesson, chỉ khi lesson chưa có URL thật.
- Upsert `dictionary_entries` từ label CSV và gắn `video_url` CloudFront.

## Bước 4: Apply SQL vào database

Nếu dùng Supabase:

1. Mở Supabase SQL Editor.
2. Dán nội dung `generated-video-url-backfill.sql`.
3. Run.

Nếu dùng `psql`:

```powershell
psql "<DATABASE_URL>" -f .\v-sign-be\scripts\media\generated-video-url-backfill.sql
```

Không commit database URL hoặc password.

## Bước 5: Kiểm tra BE API

Sau khi DB update:

```powershell
cd D:\V-sign_EXE101_Project\v-sign-be
mvn.cmd spring-boot:run
```

Kiểm tra các API:

```txt
GET http://localhost:8080/api/v1/lessons/lesson-greetings-1
GET http://localhost:8080/api/v1/dictionary?size=10
GET http://localhost:8080/api/v1/learning/practice-items?size=10
```

Kỳ vọng:

- `videoUrl` là CloudFront URL.
- Không có URL dạng `s3.amazonaws.com`.
- FE tự render video trong lesson/dictionary modal.

## Bước 6: Kiểm tra bảo mật S3

Direct S3 object URL phải bị chặn:

```txt
https://<bucket>.s3.ap-southeast-1.amazonaws.com/videos/W00489.mp4
```

Kỳ vọng: `AccessDenied`.

CloudFront URL phải xem được:

```txt
https://<CloudFrontDomainName>/videos/W00489.mp4
```

Kỳ vọng: video load được.

## Bước 7: Sau này nếu video premium cần chặn theo tài khoản

Private S3 + OAC là đúng cho bảo mật origin và tối ưu chi phí/tốc độ, nhưng chưa đủ cho authorization theo user.

Khi cần chặn premium thực sự:

1. Giữ S3 private + OAC.
2. Đổi CloudFront behavior sang signed URL/signed cookie.
3. BE kiểm tra JWT/subscription rồi cấp URL ngắn hạn.
4. FE chỉ render URL được BE cấp.
