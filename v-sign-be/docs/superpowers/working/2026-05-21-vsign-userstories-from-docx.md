# V-SIGN User Stories (Extracted from DOCX)

Source: docs/V-SIGN_UserStories_Full.docx
Generated: 2026-05-21 16:00:44 +07:00

V-SIGN
User Stories & Acceptance Criteria
Vietnamese Sign Language AI Learning Ecosystem
EXE201 â€” 4-Week Sprint Project
75 User Stories | 249 Acceptance Criteria | 7 Epics | May 2026
V-SIGN â€” User Story Backlog
**Format:** As a `[Actor]`, I want `[Action]`, so that `[Benefit]`
**Priority:** ðŸ”´ Must Have | ðŸŸ¡ Should Have | ðŸŸ¢ Could Have | âšª Won't Have (v1)
**Points:** Fibonacci scale (1, 2, 3, 5, 8, 13)
EPIC 1: Authentication & Profile
EPIC 2: Content Management (Learning)
EPIC 3: Assessment
3A â€” MCQ Quiz
3B â€” Timed Mock Test
3C â€” AI Sign Quiz
EPIC 4: Gamification
4A â€” XP System
4B â€” Streak System
4C â€” Leaderboard
4D â€” Badges
EPIC 5: VSL Dictionary
EPIC 6: Monetization (Freemium & Payment)
EPIC 7: Admin Panel
Story Summary
Sprint Suggestion (4 Tuáº§n)
V-SIGN â€” User Stories & Acceptance Criteria (Part 1/2)
Epic 1: Authentication | Epic 2: Learning | Epic 3: Assessment
EPIC 1: Authentication & Profile
US-01 â€” ÄÄƒng kÃ½ tÃ i khoáº£n
Story: As a Guest, I want to register with email & password, so that I can create an account and start learning.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: Given a valid unique email and password â‰¥ 8 characters â†’ account is created, JWT token returned, user redirected to home.
AC2: Given an already-registered email â†’ system returns error "Email nÃ y Ä‘Ã£ Ä‘Æ°á»£c sá»­ dá»¥ng."
AC3: Given password < 8 characters or missing uppercase/number â†’ inline validation error shown before submission.
AC4: Given invalid email format â†’ form cannot be submitted, error shown under email field.
US-02 â€” ÄÄƒng nháº­p
Story: As a Guest, I want to log in with email & password, so that I can access my personal learning data.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: Given correct email and password â†’ JWT token issued, user redirected to dashboard.
AC2: Given wrong password (â‰¤ 4 attempts) â†’ error "Sai máº­t kháº©u", attempt counter shown.
AC3: Given non-existent email â†’ error "TÃ i khoáº£n khÃ´ng tá»“n táº¡i."
AC4: Given account is deactivated â†’ error "TÃ i khoáº£n Ä‘Ã£ bá»‹ khÃ³a. LiÃªn há»‡ há»— trá»£."
US-03 â€” ÄÄƒng nháº­p Google OAuth
Story: As a Guest, I want to log in via Google OAuth, so that I can sign in quickly without a password.
Priority: ðŸŸ¡ Should Have | Points: 5
Acceptance Criteria:
AC1: Given user clicks "ÄÄƒng nháº­p vá»›i Google" â†’ redirected to Google consent screen.
AC2: Given successful Google auth â†’ account auto-created if first time, JWT returned, redirected to dashboard.
AC3: Given user cancels Google consent â†’ returned to login page, no account created.
AC4: Given Google account email already registered manually â†’ accounts merged, login succeeds.
US-04 â€” Xem Profile
Story: As a User, I want to view my profile page, so that I can see my account info, streak, and XP.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: Profile page displays: full name, avatar, account type (Basic/Premium), total XP, current streak, longest streak.
AC2: If account is Premium, subscription end date is shown.
AC3: Earned badges are displayed in a grid on the profile page.
AC4: Page loads within 2 seconds.
US-05 â€” Chá»‰nh sá»­a Profile
Story: As a User, I want to update my display name and avatar, so that I can personalize my account.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: Given user updates name and saves â†’ new name reflected immediately across all pages.
AC2: Given user uploads a new avatar (JPG/PNG â‰¤ 2MB) â†’ image saved to S3, displayed on profile.
AC3: Given file > 2MB â†’ error "áº¢nh khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 2MB."
AC4: Given empty name field â†’ save button is disabled.
US-06 â€” Äá»•i máº­t kháº©u
Story: As a User, I want to change my password, so that I can keep my account secure.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: Given correct current password and valid new password â†’ password updated, session remains active.
AC2: Given wrong current password â†’ error "Máº­t kháº©u hiá»‡n táº¡i khÃ´ng Ä‘Ãºng."
AC3: Given new password same as current â†’ error "Máº­t kháº©u má»›i pháº£i khÃ¡c máº­t kháº©u cÅ©."
AC4: Given new password < 8 characters â†’ validation error shown.
US-07 â€” ÄÄƒng xuáº¥t
Story: As a User, I want to log out, so that my account is safe on shared devices.
Priority: ðŸ”´ Must Have | Points: 1
Acceptance Criteria:
AC1: Given user clicks "ÄÄƒng xuáº¥t" â†’ JWT token invalidated, redirected to login page.
AC2: After logout, navigating to a protected route redirects back to login.
AC3: Local storage/session storage is cleared on logout.
US-08 â€” QuÃªn máº­t kháº©u
Story: As a Guest, I want to request a password reset via email, so that I can recover my account.
Priority: ðŸŸ¡ Should Have | Points: 5
Acceptance Criteria:
AC1: Given registered email submitted â†’ reset email sent within 1 minute with a link valid for 30 minutes.
AC2: Given unregistered email â†’ generic message shown (do not reveal if email exists).
AC3: Given user clicks valid reset link â†’ directed to set-new-password form.
AC4: Given expired reset link â†’ error "LiÃªn káº¿t Ä‘Ã£ háº¿t háº¡n. Vui lÃ²ng yÃªu cáº§u láº¡i."
EPIC 2: Content Management (Learning)
US-09 â€” Xem danh sÃ¡ch Unit
Story: As a User, I want to see all Units listed with thumbnails, so that I understand what topics are available.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: All published Units (is_published = TRUE) are displayed in order by order_index.
AC2: Each Unit card shows: thumbnail, title, number of chapters.
AC3: Unpublished Units are not visible to non-admin users.
AC4: Page renders correctly on mobile (responsive layout).
US-10 â€” Xem Chapter trong Unit
Story: As a User, I want to see Chapters inside a Unit, so that I can understand the learning structure.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: All published Chapters inside the selected Unit are listed in order_index order.
AC2: Each Chapter card shows: title, number of lessons, user's completion progress (%).
AC3: Chapters with is_premium = TRUE and user is Basic â†’ lock icon displayed.
US-11 â€” Hiá»ƒn thá»‹ khÃ³a Chapter Premium
Story: As a Basic User, I want to see a lock icon on premium Chapters, so that I know which content requires upgrade.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: Chapter with is_premium = TRUE and account_type = BASIC â†’ lock icon (ðŸ”’) shown on card.
AC2: Lock icon is visible before user clicks the chapter (no need to enter first).
AC3: Premium User sees no lock icon on any chapter.
AC4: First chapter (free chapter) always shows unlocked regardless of account type.
US-12 â€” Xem danh sÃ¡ch Lesson
Story: As a User, I want to see Lessons inside a Chapter listed in order, so that I can follow a structured path.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: Lessons displayed in order_index order with title, duration, and completion status badge.
AC2: Completed lessons show âœ… badge, in-progress show ðŸ”„, not started show â­•.
AC3: Lessons after a locked lesson are dimmed and unclickable until the previous is completed.
US-13 â€” Xem video bÃ i há»c
Story: As a User, I want to watch a lesson video hosted on S3, so that I can learn VSL signs at my own pace.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Video loads via presigned S3 URL (not proxied through Spring Boot).
AC2: Video player supports: play/pause, seek, volume control, fullscreen.
AC3: Given S3 URL fails to load â†’ placeholder shown: "Video táº¡m thá»i khÃ´ng kháº£ dá»¥ng."
AC4: Video starts from last saved position if user previously watched it partially.
US-14 â€” LÆ°u tiáº¿n Ä‘á»™ tá»± Ä‘á»™ng
Story: As a User, I want my progress saved automatically when I finish a video, so that I don't lose my place.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: When video ends (onVideoEnded) â†’ API called to update User_Progress.status = COMPLETED.
AC2: When user pauses/leaves mid-video â†’ completion_pct saved, status = IN_PROGRESS.
AC3: Progress is reflected immediately on the Chapter screen (no page refresh needed).
AC4: XP is awarded only once per lesson completion (idempotent).
US-15 â€” Progress bar theo Chapter
Story: As a User, I want to see a progress bar per Chapter, so that I can track how many lessons I've completed.
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: Progress bar = (completed lessons / total lessons) Ã— 100%.
AC2: Progress bar updates in real time after completing a lesson.
AC3: 100% completion â†’ chapter card shows a "HoÃ n thÃ nh" badge.
US-16 â€” Má»Ÿ khÃ³a bÃ i há»c káº¿ tiáº¿p
Story: As a User, I want the next lesson to unlock after completing the current one, so that content is sequential.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: Given lesson N is COMPLETED â†’ lesson N+1 becomes clickable immediately.
AC2: Given lesson N is IN_PROGRESS or NOT_STARTED â†’ lesson N+1 is locked (dimmed, not clickable).
AC3: First lesson in each Chapter is always unlocked by default.
AC4: Lesson unlock does not require page refresh.
US-17 â€” Redirect Paywall khi click Chapter Premium
Story: As a Basic User, I want to be redirected to the upgrade page when clicking a locked Chapter.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: Basic User clicking a premium Chapter â†’ Paywall modal/screen appears (not a full page redirect that loses context).
AC2: Paywall shows: benefits of Premium, price (49k/thÃ¡ng), and a CTA button "NÃ¢ng cáº¥p ngay".
AC3: Clicking "NÃ¢ng cáº¥p ngay" navigates to the payment flow (UC-02).
AC4: Clicking "ÄÃ³ng" dismisses the paywall and returns to the Chapter list.
US-18 â€” Resume bÃ i há»c dang dá»Ÿ
Story: As a User, I want to resume a lesson I didn't finish, so that I don't have to restart from the beginning.
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: Given completion_pct > 0 â†’ video player auto-seeks to last saved timestamp on load.
AC2: A toast shows: "Tiáº¿p tá»¥c tá»« 02:35" with option to "Xem láº¡i tá»« Ä‘áº§u."
AC3: Given completion_pct = 0 â†’ video starts from the beginning.
EPIC 3: Assessment
3A â€” MCQ Quiz
US-19 â€” LÃ m Quiz MCQ
Story: As a User, I want to take a multiple-choice quiz after a lesson, so that I can test my understanding.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Quiz displays questions one at a time with 4 answer options (A/B/C/D).
AC2: User can change answer before submitting.
AC3: Submit button only enabled when all questions have a selected answer.
AC4: Given Basic User on a premium lesson's quiz â†’ blocked by Paywall.
US-20 â€” Xem Ä‘iá»ƒm sau Quiz
Story: As a User, I want to see my score immediately after submitting a quiz.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: After submission â†’ score displayed as: "X/N cÃ¢u Ä‘Ãºng â€” Y Ä‘iá»ƒm."
AC2: If score â‰¥ passing_score â†’ "Äáº¡t âœ…" badge shown and next lesson unlocks.
AC3: If score < passing_score â†’ "ChÆ°a Ä‘áº¡t âŒ" with "LÃ m láº¡i" button.
AC4: XP is awarded immediately if passed (shown with animation).
US-21 â€” Review Ä‘Ã¡p Ã¡n
Story: As a User, I want to review correct and incorrect answers after finishing a quiz.
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: Each question shows: user's selected answer, correct answer, and explanation (if configured).
AC2: Wrong answers highlighted in red, correct in green.
AC3: Review screen accessible from the result screen without retaking the quiz.
US-22 â€” LÃ m láº¡i Quiz
Story: As a User, I want to retake a quiz I failed, so that I can improve my score.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: "LÃ m láº¡i" button available on result screen if not passed.
AC2: On retake, questions are reshuffled and answer options reshuffled.
AC3: Best score (highest across all attempts) is saved to User_Progress.score.
AC4: XP is awarded only on first passing attempt (not on every retry).
3B â€” Timed Mock Test
US-23 â€” Thi thá»­ cÃ³ báº¥m giá»
Story: As a User, I want to take a timed mock test with a countdown timer, so that I can simulate a real exam.
Priority: ðŸ”´ Must Have | Points: 8
Acceptance Criteria:
AC1: Timer starts immediately when the test begins, counts down from time_limit_seconds.
AC2: Timer is displayed prominently (top of screen) and turns red when < 60 seconds remain.
AC3: Timer runs entirely client-side; server validates duration on submission.
AC4: Timer persists if user accidentally refreshes (stored in sessionStorage).
US-24 â€” Auto-submit khi háº¿t giá»
Story: As a User, I want my quiz to auto-submit when the timer runs out.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: When countdown = 0 â†’ quiz auto-submits all answered questions.
AC2: Unanswered questions are scored as incorrect (no additional penalty).
AC3:Quiz_Attempts.status saved as TIMED_OUT (distinguishable from COMPLETED).
AC4: Auto-submit toast: "â° Háº¿t giá»! Äang ná»™p bÃ i tá»± Ä‘á»™ng..."
US-25 â€” Äiá»u hÆ°á»›ng tá»± do giá»¯a cÃ¡c cÃ¢u
Story: As a User, I want to navigate between questions freely during a timed test.
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: Question navigator panel shows all question numbers; answered ones highlighted.
AC2: User can jump to any question by clicking its number.
AC3: Selected answer is saved in local state when navigating away.
US-26 â€” Cáº£nh bÃ¡o cÃ¢u chÆ°a tráº£ lá»i
Story: As a User, I want to see which questions I haven't answered before submitting.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: Clicking "Ná»™p bÃ i" with unanswered questions â†’ confirmation dialog: "CÃ²n X cÃ¢u chÆ°a tráº£ lá»i. Báº¡n cÃ³ cháº¯c muá»‘n ná»™p?"
AC2: Unanswered question numbers are highlighted in yellow in the navigator.
AC3: User can cancel and return to complete missing answers.
3C â€” AI Sign Quiz
US-27 â€” Luyá»‡n táº­p AI (AI Quiz)
Story: As a Premium User, I want to practice sign language by performing signs in front of my camera, so that I get real-time AI feedback.
Priority: ðŸ”´ Must Have | Points: 13
Acceptance Criteria:
AC1: Camera activates only after user explicitly grants permission.
AC2: MediaPipe detects hand landmarks in real time with latency â‰¤ 200ms per frame.
AC3: A sign is confirmed as correct when confidence â‰¥ 0.85 across â‰¥ 3 consecutive frames.
AC4: User receives instant visual feedback (âœ…/âŒ) within 500ms of confirmed detection.
AC5: Quiz submits only metadata to server (no video/image frames sent).
US-28 â€” Privacy-safe AI (no video to server)
Story: As a Premium User, I want hand sign recognition done on my device, so that my privacy is protected.
Priority: ðŸ”´ Must Have | Points: 8
Acceptance Criteria:
AC1: No image or video frame is transmitted to the Spring Boot API at any point.
AC2: Only { predicted_sign, confidence_score, is_correct } metadata is sent to server after quiz ends.
AC3: Network tab in browser DevTools shows no image/video payloads to backend.
AC4: MediaPipe WASM model runs entirely in browser context (verifiable via Source tab).
US-29 â€” Thanh confidence real-time
Story: As a Premium User, I want to see a confidence indicator while performing a sign.
Priority: ðŸŸ¡ Should Have | Points: 5
Acceptance Criteria:
AC1: Confidence bar (0â€“100%) updates live on every frame (â‰¤ 200ms refresh).
AC2: Bar color: Red (0â€“50%), Yellow (50â€“84%), Green (85â€“100%).
AC3: When confidence locks at â‰¥ 85% â†’ bar freezes and result is confirmed.
US-30 â€” Pháº£n há»“i tá»©c thÃ¬ sau má»—i kÃ½ hiá»‡u
Story: As a Premium User, I want instant feedback after each sign attempt.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Correct sign â†’ green overlay âœ… "ÄÃºng rá»“i!" + sound effect + auto-advance to next question after 1.5s.
AC2: Incorrect sign (after timeout) â†’ red overlay âŒ "Thá»­ láº¡i!" with hint image.
AC3: User can skip a question (max 2 skips per quiz session).
US-31 â€” Cáº£nh bÃ¡o Ã¡nh sÃ¡ng yáº¿u
Story: As a Premium User, I want a warning when lighting is too low for hand detection.
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: If no hand detected for > 5 consecutive seconds â†’ warning shown: "KhÃ³ nháº­n diá»‡n. Thá»­ tÄƒng Ã¡nh sÃ¡ng hoáº·c Ä‘á»•i ná»n."
AC2: Warning auto-dismisses when hand is detected again.
AC3: Timer pauses during the no-detection period (if timed quiz).
US-32 â€” Preview AI Quiz cho Basic User
Story: As a Basic User, I want to see a preview of the AI Quiz feature, so that I feel motivated to upgrade.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: Basic User sees a blurred/locked AI Quiz section with label "TÃ­nh nÄƒng Premium."
AC2: A teaser message describes the feature: "Luyá»‡n táº­p kÃ½ hiá»‡u trá»±c tiáº¿p vá»›i AI."
AC3: CTA button "NÃ¢ng cáº¥p Ä‘á»ƒ tráº£i nghiá»‡m" links to Payment flow.
V-SIGN â€” User Stories & Acceptance Criteria (Part 2/2)
Epic 4: Gamification | Epic 5: Dictionary | Epic 6: Monetization | Epic 7: Admin
EPIC 4: Gamification
4A â€” XP System
US-33 â€” Nháº­n XP sau bÃ i há»c
Story: As a User, I want to earn XP when I complete a lesson, so that I feel rewarded for my progress.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: On User_Progress.status = COMPLETED â†’ XP_Logs entry created with source_type = LESSON_COMPLETE, xp_earned = lesson.xp_reward.
AC2: XP is awarded only once per lesson (subsequent completions/retakes do not re-award).
AC3: Total XP on profile updates immediately after lesson completion.
US-34 â€” Nháº­n XP sau Quiz
Story: As a User, I want to earn XP when I complete a quiz, so that assessments feel meaningful.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: On quiz passed â†’ XP_Logs entry with source_type = QUIZ_COMPLETE, xp_earned = quiz.xp_reward.
AC2: XP is awarded only on first passing attempt (not on retries).
AC3: Failed quiz â†’ no XP awarded.
US-35 â€” Animation XP pop-up
Story: As a User, I want to see an XP animation pop up after completing activities.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: After earning XP â†’ floating "+X XP" animation appears on screen for 2 seconds.
AC2: Animation is non-blocking (user can interact with other elements).
AC3: Animation reflects actual XP value (not a hardcoded number).
US-36 â€” Tá»•ng XP trÃªn Profile
Story: As a User, I want to see my total XP on my profile, so that I can track overall progress.
Priority: ðŸ”´ Must Have | Points: 1
Acceptance Criteria:
AC1: Profile displays total XP as sum of all XP_Logs.xp_earned for the user.
AC2: XP updates in real time after each earning event (no manual refresh needed).
4B â€” Streak System
US-37 â€” XÃ¢y dá»±ng Streak hÃ ng ngÃ y
Story: As a User, I want to build a daily learning streak, so that I stay motivated every day.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Completing at least 1 lesson or quiz per calendar day (UTC+7) increments current_streak by 1.
AC2: Streak increments only once per day regardless of how many activities completed.
AC3:last_activity_date is updated to today (UTC+7) on each qualifying activity.
AC4: Streak logic uses lazy evaluation: reset happens on next activity, not via background job.
US-38 â€” Hiá»ƒn thá»‹ Streak tÄƒng
Story: As a User, I want my streak to visually increase when I learn on consecutive days.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: After streak increments â†’ ðŸ”¥ fire animation shows "Streak: X ngÃ y!" on result screen.
AC2: Streak count on dashboard/profile header updates immediately.
AC3: On streak milestone days (7, 14, 30) â†’ special celebration animation triggered.
US-39 â€” ThÃ´ng bÃ¡o reset Streak rÃµ rÃ ng
Story: As a User, I want to clearly see when my streak resets, so that I never feel cheated.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: If streak has reset â†’ message shown: "Streak Ä‘Ã£ bá»‹ reset. HÃ´m nay lÃ NgÃ y 1 má»›i â€” cá»‘ lÃªn!"
AC2: Dashboard shows a daily reminder widget: "Há»c hÃ´m nay Ä‘á»ƒ giá»¯ streak ðŸ”¥" (if not yet studied today).
AC3: Streak reset message is shown once per session, not repeatedly.
AC4: Help tooltip explains the reset rule clearly: "Streak reset náº¿u báº¡n bá» qua 1 ngÃ y (tÃ­nh theo giá» VN UTC+7)."
US-40 â€” Longest Streak trÃªn Profile
Story: As a User, I want my longest streak recorded on my profile.
Priority: ðŸŸ¡ Should Have | Points: 1
Acceptance Criteria:
AC1:longest_streak displays separately from current_streak on profile.
AC2:longest_streak only increases, never decreases (even when current streak resets).
US-41 â€” XP Multiplier theo Streak Milestone
Story: As a User, I want bonus XP multipliers at streak milestones (7, 14, 30 days).
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: Streak 7â€“13 days â†’ XP Ã— 1.2; 14â€“29 days â†’ Ã— 1.5; 30+ days â†’ Ã— 2.0.
AC2: Bonus XP logged separately in XP_Logs with source_type = STREAK_BONUS.
AC3: Multiplier displayed on result screen: "Streak Bonus: +Y XP (Ã—1.5)".
AC4: Multiplier applies to Lesson XP and Quiz XP (not to Badge XP).
4C â€” Leaderboard
US-42 â€” Báº£ng xáº¿p háº¡ng tuáº§n
Story: As a User, I want to see a weekly leaderboard ranked by XP.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Leaderboard shows top 50 users ranked by XP earned in the current calendar week (Monâ€“Sun UTC+7).
AC2: Each row shows: rank, avatar, name, weekly XP.
AC3: Leaderboard resets every Monday 00:00 UTC+7 (historical data kept in Leaderboard_Snapshots).
AC4: Page loads leaderboard data within 2 seconds.
US-43 â€” Báº£ng xáº¿p háº¡ng thÃ¡ng
Story: As a User, I want to see a monthly leaderboard.
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: Monthly tab shows top 50 users by XP earned in current calendar month.
AC2: User can toggle between Weekly and Monthly tabs without page reload.
AC3: Monthly leaderboard resets on the 1st of each month.
US-44 â€” Highlight vá»‹ trÃ­ cá»§a mÃ¬nh
Story: As a User, I want to see my own rank highlighted on the leaderboard.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: Current user's row is highlighted with a distinct background color.
AC2: If user is outside top 50 â†’ a pinned row at the bottom shows their rank and XP.
AC3: "Báº¡n Ä‘ang á»Ÿ háº¡ng #X" label shown above or below the leaderboard.
4D â€” Badges
US-45 â€” Badge cho Streak Milestone
Story: As a User, I want to earn a badge when I hit streak milestones (7, 14, 30 days).
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: At streak 7 â†’ Badge "ðŸ”¥ Tuáº§n Lá»­a" awarded. At 14 â†’ "âš¡ KiÃªn TrÃ¬". At 30 â†’ "ðŸ† Báº¥t Khuáº¥t".
AC2: Badge award is idempotent (same badge not awarded twice).
AC3: Badge notification popup shown immediately when milestone reached.
US-46 â€” Badge cho XP Milestone
Story: As a User, I want to earn a badge when I reach XP milestones.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: At 100 XP, 500 XP, 1000 XP, 5000 XP â†’ corresponding badge awarded.
AC2: Badge granted within the same API response that credited the XP.
US-47 â€” Badge bÃ i há»c Ä‘áº§u tiÃªn
Story: As a User, I want to earn a badge for completing my first lesson.
Priority: ðŸŸ¡ Should Have | Points: 1
Acceptance Criteria:
AC1: On first User_Progress.status = COMPLETED ever â†’ Badge "ðŸŒ± Khá»Ÿi Äáº§u" awarded.
AC2: Badge shown with welcome message: "ChÃ o má»«ng! Báº¡n Ä‘Ã£ hoÃ n thÃ nh bÃ i há»c Ä‘áº§u tiÃªn."
US-48 â€” Badge Ä‘iá»ƒm tuyá»‡t Ä‘á»‘i
Story: As a User, I want to earn a badge for getting a perfect score on a quiz.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1:correct_answers = total_questions on a quiz attempt â†’ Badge "â­ HoÃ n Háº£o" awarded.
AC2: Badge awarded once per quiz type (not once per attempt).
US-49 â€” Xem táº¥t cáº£ Badge trÃªn Profile
Story: As a User, I want to view all badges I've earned on my profile.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: Profile shows a badge gallery with earned badges in color and unearned badges grayed out.
AC2: Hovering/clicking a badge shows its name and the condition to earn it.
AC3: Badges sorted by earned_at descending (newest first).
EPIC 5: VSL Dictionary
US-50 â€” Duyá»‡t tá»« Ä‘iá»ƒn khÃ´ng cáº§n Ä‘Äƒng nháº­p
Story: As a Guest, I want to browse the VSL dictionary without logging in.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1:/dictionary route is publicly accessible without JWT token.
AC2: All published dictionary entries are returned by the public API endpoint.
AC3: No "Login required" gate appears on the dictionary page.
US-51 â€” Lá»c tá»« Ä‘iá»ƒn theo danh má»¥c
Story: As a User, I want to browse VSL signs by category.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: Category list loaded from GET /api/dictionary/categories.
AC2: Selecting a category filters the grid to show only entries in that category.
AC3: "Táº¥t cáº£" option resets the filter and shows all entries.
AC4: Category filter is reflected in the URL (e.g., ?category=ChÃ o+há»i) for shareability.
US-52 â€” TÃ¬m kiáº¿m tá»« Ä‘iá»ƒn
Story: As a User, I want to search for a specific sign by name.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: Search is triggered on input (debounced 300ms) or on pressing Enter.
AC2: Results match sign_name containing the search keyword (case-insensitive).
AC3: No results â†’ message: "KhÃ´ng tÃ¬m tháº¥y kÃ½ hiá»‡u nÃ o cho '[keyword]'."
AC4: Search works in combination with category filter.
US-53 â€” Xem video minh há»a kÃ½ hiá»‡u
Story: As a User, I want to watch a video demonstration of each sign.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: Video loads within 3 seconds on standard broadband.
AC2: Video autoplays (muted) on the detail page; user can unmute.
AC3: Video streams via S3 presigned URL (not proxied by backend).
AC4: If video unavailable â†’ placeholder image with message "Video Ä‘ang Ä‘Æ°á»£c cáº­p nháº­t."
US-54 â€” Má»©c Ä‘á»™ khÃ³ cá»§a kÃ½ hiá»‡u
Story: As a User, I want to see a difficulty level for each sign.
Priority: ðŸŸ¡ Should Have | Points: 1
Acceptance Criteria:
AC1: Difficulty badge shown on each dictionary card: Beginner (ðŸŸ¢) / Intermediate (ðŸŸ¡) / Advanced (ðŸ”´).
AC2: User can filter dictionary by difficulty level.
US-55 â€” NÃºt "Luyá»‡n táº­p ngay" tá»« Tá»« Ä‘iá»ƒn
Story: As a Premium User, I want a "Practice this sign" button on dictionary entries.
Priority: ðŸŸ¢ Could Have | Points: 3
Acceptance Criteria:
AC1: "Luyá»‡n táº­p ngay" button visible on detail page for Premium Users.
AC2: Button links to the AI Quiz lesson most relevant to that sign (by sign_label match).
AC3: If no matching lesson exists â†’ button hidden or shows "ChÆ°a cÃ³ bÃ i luyá»‡n táº­p."
AC4: Basic User sees button as locked with "Premium" badge.
EPIC 6: Monetization
US-56 â€” Hiá»ƒn thá»‹ Paywall
Story: As a Basic User, I want to see a clear paywall screen when accessing premium content.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: Paywall appears as a modal overlay (not a full redirect) to preserve context.
AC2: Paywall lists â‰¥ 3 Premium benefits clearly (AI Quiz, all Chapters, etc.).
AC3: CTA button "NÃ¢ng cáº¥p Premium â€” 49.000Ä‘/thÃ¡ng" is prominent and clickable.
AC4: "Äá»ƒ sau" / "ÄÃ³ng" button dismisses the paywall without data loss.
US-57 â€” Xem gÃ³i Premium
Story: As a Basic User, I want to view premium pricing plans.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: Pricing page shows at minimum: Monthly (49.000Ä‘) plan with features list.
AC2: Plan selection is visually distinct (selected plan highlighted with border).
AC3: "Tiáº¿p tá»¥c thanh toÃ¡n" button is disabled until a plan is selected.
US-58 â€” Thanh toÃ¡n MoMo QR
Story: As a Basic User, I want to pay for Premium using MoMo QR code.
Priority: ðŸ”´ Must Have | Points: 8
Acceptance Criteria:
AC1: Clicking "Thanh toÃ¡n MoMo" â†’ QR code generated and displayed within 3 seconds.
AC2: QR code has a 5-minute countdown timer shown below it.
AC3: System polls GET /api/payments/status/{id} every 3 seconds for payment confirmation.
AC4: On status = SUCCESS from webhook â†’ account upgraded within 5 seconds of payment.
AC5:Payment_Transactions.provider_transaction_id stored for reconciliation.
US-59 â€” Thanh toÃ¡n ZaloPay QR
Story: As a Basic User, I want to pay for Premium using ZaloPay QR code.
Priority: ðŸŸ¡ Should Have | Points: 5
Acceptance Criteria:
AC1: ZaloPay option available alongside MoMo on payment screen.
AC2: ZaloPay QR generated using ZaloPay's create-order API, displayed within 3 seconds.
AC3: Webhook from ZaloPay handled and verified (HMAC signature check).
AC4: Behavior after success identical to MoMo flow (US-58 AC4).
US-60 â€” NÃ¢ng cáº¥p tÃ i khoáº£n ngay sau thanh toÃ¡n
Story: As a Basic User, I want my account upgraded to Premium immediately after successful payment.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1:Users.account_type updated to PREMIUM only after Payment_Transactions.status = SUCCESS confirmed via webhook.
AC2:Subscriptions record created with start_date = today, end_date = today + 30 days.
AC3: User can access premium content within 10 seconds of payment confirmation.
AC4: If webhook fails or is delayed â†’ account is NOT upgraded prematurely.
US-61 â€” MÃ n hÃ¬nh xÃ¡c nháº­n thanh toÃ¡n
Story: As a User, I want to see a payment confirmation screen after success.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1: Success screen shows: "ChÃºc má»«ng! Báº¡n Ä‘Ã£ lÃ thÃ nh viÃªn Premium ðŸŽ‰", plan details, and expiry date.
AC2: CTA button "KhÃ¡m phÃ¡ ngay" navigates to the full Unit list.
AC3: Confirmation screen accessible even if user navigates away and comes back.
US-62 â€” ThÃ´ng bÃ¡o lá»—i thanh toÃ¡n
Story: As a User, I want a clear error message if my payment fails.
Priority: ðŸ”´ Must Have | Points: 2
Acceptance Criteria:
AC1:status = FAILED from webhook â†’ error screen: "Thanh toÃ¡n khÃ´ng thÃ nh cÃ´ng. Vui lÃ²ng thá»­ láº¡i."
AC2: Error screen offers: "Thá»­ láº¡i" (regenerate QR) and "Chá»n phÆ°Æ¡ng thá»©c khÃ¡c."
AC3: Failed transaction is logged in Payment_Transactions with status = FAILED for audit.
AC4:Users.account_type remains BASIC after a failed payment.
US-63 â€” Xem tráº¡ng thÃ¡i Subscription
Story: As a Premium User, I want to view my subscription status and expiry date.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: Profile shows subscription badge: "Premium â€” Háº¿t háº¡n: DD/MM/YYYY."
AC2: If expiry is within 7 days â†’ yellow warning: "GÃ³i sáº¯p háº¿t háº¡n. Gia háº¡n ngay."
AC3: After expiry â†’ account_type auto-reverts to BASIC (via scheduled job).
US-64 â€” Lá»‹ch sá»­ thanh toÃ¡n
Story: As a User, I want to view my payment history.
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: Payment history shows: date, provider, amount, status (SUCCESS/FAILED/PENDING).
AC2: Transactions sorted by created_at descending.
AC3:provider_transaction_id displayed for each transaction (for user-side reconciliation).
EPIC 7: Admin Panel
US-65 â€” Quáº£n lÃ½ Unit
Story: As an Admin, I want to create, edit, and delete Units.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Admin can create a Unit with: title, description, thumbnail (uploaded to S3), order_index.
AC2: Admin can toggle is_published to show/hide a Unit for learners.
AC3: Deleting a Unit with existing Chapters â†’ confirmation warning shown; cascade delete or block enforced.
AC4:order_index can be reordered via drag-and-drop or input field.
US-66 â€” Quáº£n lÃ½ Chapter & is_premium
Story: As an Admin, I want to create Chapters and set is_premium to control content access.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Admin can create a Chapter inside a Unit with: title, order_index, is_premium toggle.
AC2: Toggling is_premium = TRUE â†’ immediately blocks Basic Users from accessing that Chapter.
AC3: Admin can move a Chapter between Units (reassign unit_id).
US-67 â€” Upload video bÃ i há»c lÃªn S3
Story: As an Admin, I want to upload lesson videos to S3 and link them to a Lesson.
Priority: ðŸ”´ Must Have | Points: 8
Acceptance Criteria:
AC1: Admin selects video file (MP4/MOV â‰¤ 500MB) â†’ uploaded directly to S3 via presigned upload URL.
AC2: Upload progress bar shown in Admin UI.
AC3: After successful upload â†’ lesson.video_url is saved automatically.
AC4: If upload fails â†’ error message shown, video_url not updated.
AC5: Admin can replace an existing video (old S3 object optionally deleted).
US-68 â€” Táº¡o cÃ¢u há»i MCQ
Story: As an Admin, I want to create MCQ quiz questions with options and correct answers.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Admin can add a question with question text, 4 options, and mark exactly 1 as correct.
AC2: System prevents saving without exactly 1 correct answer selected.
AC3: Admin can reorder questions via order_index.
AC4: Admin can preview the quiz from the learner's perspective.
US-69 â€” Táº¡o cÃ¢u há»i AI Quiz
Story: As an Admin, I want to create AI Quiz questions by specifying a sign_label.
Priority: ðŸ”´ Must Have | Points: 3
Acceptance Criteria:
AC1: Admin specifies sign_label (e.g., "xin_chao") matching the AI model's label vocabulary.
AC2: Admin can optionally add a hint image or description for the learner.
AC3:question_type saved as AI_SIGN in database.
AC4: Validation: sign_label must not be empty and must match a predefined label list.
US-70 â€” Quáº£n lÃ½ Tá»« Ä‘iá»ƒn VSL
Story: As an Admin, I want to add and edit Dictionary entries.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Admin can create/edit: sign_name, category, description, video_url, thumbnail_url, difficulty_level.
AC2: Video upload follows same S3 flow as lesson videos.
AC3: Admin can delete a Dictionary entry (soft delete or hard delete with confirmation).
AC4: Changes reflect on the public Dictionary page immediately.
US-71 â€” Xem danh sÃ¡ch ngÆ°á»i dÃ¹ng
Story: As an Admin, I want to view a list of all registered users.
Priority: ðŸŸ¡ Should Have | Points: 3
Acceptance Criteria:
AC1: User list shows: name, email, account_type, registration date, last active.
AC2: Admin can search users by email or name.
AC3: Admin can filter by account_type (Basic / Premium).
AC4: List is paginated (20 users per page).
US-72 â€” Xem subscription status cá»§a user
Story: As an Admin, I want to see each user's subscription status.
Priority: ðŸŸ¡ Should Have | Points: 2
Acceptance Criteria:
AC1: User detail page shows: subscription status (ACTIVE/EXPIRED), start date, end date.
AC2: Admin can see the linked payment_transaction_id for each subscription.
US-73 â€” Xem toÃ n bá»™ giao dá»‹ch thanh toÃ¡n
Story: As an Admin, I want to view all payment transactions with status.
Priority: ðŸ”´ Must Have | Points: 5
Acceptance Criteria:
AC1: Transaction table shows: user email, provider, amount, provider_transaction_id, status, date.
AC2: Admin can filter by status (PENDING / SUCCESS / FAILED / REFUNDED).
AC3: Admin can search by provider_transaction_id to find a specific transaction.
AC4:raw_payload (webhook body) viewable on transaction detail for debugging.
US-74 â€” Thá»§ cÃ´ng cáº­p nháº­t tráº¡ng thÃ¡i giao dá»‹ch
Story: As an Admin, I want to manually trigger a subscription update for a failed webhook case.
Priority: ðŸŸ¡ Should Have | Points: 5
Acceptance Criteria:
AC1: Admin can mark a PENDING transaction as SUCCESS manually (with confirmation dialog).
AC2: Doing so triggers the same logic as webhook: create Subscription, upgrade account_type.
AC3: Manual override is logged with updated_by = admin_id and timestamp in audit log.
AC4: Only Super Admin role can perform manual override (not regular Admin).
US-75 â€” Dashboard KPI Admin
Story: As an Admin, I want a dashboard with total users, active subscriptions, and daily active learners.
Priority: ðŸŸ¡ Should Have | Points: 8
Acceptance Criteria:
AC1: Dashboard shows: Total Users, Premium Users count, Daily Active Users (DAU), Weekly Revenue.
AC2: DAU calculated from users with User_Progress.updated_at = today.
AC3: Revenue chart shows daily/weekly revenue from successful Payment_Transactions.
AC4: Data refreshes every 5 minutes (or on manual refresh).
Acceptance Criteria Summary
