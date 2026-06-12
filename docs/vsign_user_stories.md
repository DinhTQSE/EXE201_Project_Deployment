# V-SIGN — User Story Backlog

> **Format:** As a `[Actor]`, I want `[Action]`, so that `[Benefit]`
> **Priority:** 🔴 Must Have | 🟡 Should Have | 🟢 Could Have | ⚪ Won't Have (v1)
> **Points:** Fibonacci scale (1, 2, 3, 5, 8, 13)

---

## EPIC 1: Authentication & Profile

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-01 | As a **Guest**, I want to register with email & password, so that I can create an account and start learning. | Guest | 🔴 | 3 |
| US-02 | As a **Guest**, I want to log in with my email & password, so that I can access my personal learning data. | Guest | 🔴 | 2 |
| US-03 | As a **Guest**, I want to log in via Google OAuth, so that I can sign up/in quickly without remembering a password. | Guest | 🟡 | 5 |
| US-04 | As a **User**, I want to view my profile page, so that I can see my account info, streak, and XP total. | Basic/Premium | 🔴 | 2 |
| US-05 | As a **User**, I want to update my display name and avatar, so that I can personalize my account. | Basic/Premium | 🟡 | 2 |
| US-06 | As a **User**, I want to change my password, so that I can keep my account secure. | Basic/Premium | 🟡 | 2 |
| US-07 | As a **User**, I want to log out, so that my account is safe on shared devices. | Basic/Premium | 🔴 | 1 |
| US-08 | As a **Guest**, I want to request a password reset via email, so that I can recover my account if I forget my password. | Guest | 🟡 | 5 |

---

## EPIC 2: Content Management (Learning)

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-09 | As a **User**, I want to see all Units listed with thumbnails and descriptions, so that I can understand what topics are available. | Basic/Premium | 🔴 | 3 |
| US-10 | As a **User**, I want to see Chapters inside a Unit, so that I can understand the learning structure. | Basic/Premium | 🔴 | 2 |
| US-11 | As a **Basic User**, I want to see a lock icon on premium Chapters, so that I know which content requires an upgrade. | Basic | 🔴 | 2 |
| US-12 | As a **User**, I want to see Lessons inside a Chapter listed in order, so that I can follow a structured learning path. | Basic/Premium | 🔴 | 2 |
| US-13 | As a **User**, I want to watch a lesson video hosted on S3, so that I can learn VSL signs at my own pace. | Basic/Premium | 🔴 | 5 |
| US-14 | As a **User**, I want my progress to be saved automatically when I finish a video, so that I don't lose my place if I come back later. | Basic/Premium | 🔴 | 3 |
| US-15 | As a **User**, I want to see a progress bar per Chapter, so that I can track how many lessons I've completed. | Basic/Premium | 🟡 | 3 |
| US-16 | As a **User**, I want the next lesson to unlock after completing the current one, so that content is delivered in a logical sequence. | Basic/Premium | 🔴 | 3 |
| US-17 | As a **Basic User**, I want to be redirected to the upgrade page when clicking a locked Chapter, so that I understand how to gain access. | Basic | 🔴 | 2 |
| US-18 | As a **User**, I want to resume a lesson I didn't finish, so that I don't have to restart from the beginning. | Basic/Premium | 🟡 | 3 |

---

## EPIC 3: Assessment

### 3A — MCQ Quiz

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-19 | As a **User**, I want to take a multiple-choice quiz after a lesson, so that I can test my understanding of the signs I learned. | Basic/Premium | 🔴 | 5 |
| US-20 | As a **User**, I want to see my score immediately after submitting a quiz, so that I know how well I performed. | Basic/Premium | 🔴 | 2 |
| US-21 | As a **User**, I want to review correct and incorrect answers after finishing a quiz, so that I can learn from my mistakes. | Basic/Premium | 🟡 | 3 |
| US-22 | As a **User**, I want to retake a quiz I failed, so that I can improve my score and unlock the next lesson. | Basic/Premium | 🟡 | 2 |

### 3B — Timed Mock Test

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-23 | As a **User**, I want to take a timed mock test with a countdown timer, so that I can simulate a real exam environment. | Basic/Premium | 🔴 | 8 |
| US-24 | As a **User**, I want my quiz to auto-submit when the timer runs out, so that my partial answers are still scored. | Basic/Premium | 🔴 | 3 |
| US-25 | As a **User**, I want to navigate between questions freely during a timed test, so that I can manage my time strategically. | Basic/Premium | 🟡 | 3 |
| US-26 | As a **User**, I want to see which questions I haven't answered before submitting, so that I don't accidentally skip questions. | Basic/Premium | 🟡 | 2 |

### 3C — AI Sign Quiz

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-27 | As a **Premium User**, I want to practice sign language by performing signs in front of my camera, so that I can get real-time AI feedback on my technique. | Premium | 🔴 | 13 |
| US-28 | As a **Premium User**, I want the system to recognize my hand signs using my device's camera without sending video to the server, so that my privacy is protected and the experience is fast. | Premium | 🔴 | 8 |
| US-29 | As a **Premium User**, I want to see a confidence indicator while performing a sign, so that I know if the camera is reading my gesture correctly. | Premium | 🟡 | 5 |
| US-30 | As a **Premium User**, I want to get instant feedback (correct/incorrect) after each sign attempt, so that I can immediately correct my technique. | Premium | 🔴 | 5 |
| US-31 | As a **Premium User**, I want a warning when lighting is too low for hand detection, so that I can adjust my environment and avoid failed attempts. | Premium | 🟡 | 3 |
| US-32 | As a **Basic User**, I want to see a preview of the AI Quiz feature locked behind Premium, so that I understand what I'm missing and feel motivated to upgrade. | Basic | 🟡 | 2 |

---

## EPIC 4: Gamification

### 4A — XP System

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-33 | As a **User**, I want to earn XP when I complete a lesson, so that I feel rewarded for my progress. | Basic/Premium | 🔴 | 3 |
| US-34 | As a **User**, I want to earn XP when I complete a quiz, so that assessments feel meaningful. | Basic/Premium | 🔴 | 2 |
| US-35 | As a **User**, I want to see an XP animation pop up after completing activities, so that earning XP feels exciting and immediate. | Basic/Premium | 🟡 | 2 |
| US-36 | As a **User**, I want to see my total XP on my profile, so that I can track my overall progress. | Basic/Premium | 🔴 | 1 |

### 4B — Streak System

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-37 | As a **User**, I want to build a daily learning streak, so that I stay motivated to learn every day. | Basic/Premium | 🔴 | 5 |
| US-38 | As a **User**, I want my streak to visually increase when I learn on consecutive days, so that I feel a sense of momentum. | Basic/Premium | 🔴 | 2 |
| US-39 | As a **User**, I want to clearly see when my streak resets (next day midnight UTC+7), so that I never feel cheated by an unexpected reset. | Basic/Premium | 🔴 | 2 |
| US-40 | As a **User**, I want my longest streak to be recorded on my profile, so that I can show off my best record. | Basic/Premium | 🟡 | 1 |
| US-41 | As a **User**, I want to earn bonus XP multipliers at streak milestones (7, 14, 30 days), so that maintaining a long streak feels increasingly rewarding. | Basic/Premium | 🟡 | 3 |

### 4C — Leaderboard

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-42 | As a **User**, I want to see a weekly leaderboard ranked by XP, so that I can compete with other learners. | Basic/Premium | 🔴 | 5 |
| US-43 | As a **User**, I want to see a monthly leaderboard, so that I can track my long-term ranking. | Basic/Premium | 🟡 | 3 |
| US-44 | As a **User**, I want to see my own rank highlighted on the leaderboard, so that I can find my position quickly. | Basic/Premium | 🟡 | 2 |

### 4D — Badges

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-45 | As a **User**, I want to earn a badge when I hit streak milestones (7, 14, 30 days), so that consistent effort is visibly recognized. | Basic/Premium | 🔴 | 3 |
| US-46 | As a **User**, I want to earn a badge when I reach XP milestones, so that overall learning volume is rewarded. | Basic/Premium | 🟡 | 2 |
| US-47 | As a **User**, I want to earn a badge for completing my first lesson, so that beginners feel welcomed and encouraged. | Basic/Premium | 🟡 | 1 |
| US-48 | As a **User**, I want to earn a badge for getting a perfect score on a quiz, so that high performance is recognized. | Basic/Premium | 🟡 | 2 |
| US-49 | As a **User**, I want to view all badges I've earned on my profile, so that I can see all my achievements in one place. | Basic/Premium | 🟡 | 2 |

---

## EPIC 5: VSL Dictionary

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-50 | As a **Guest**, I want to browse the VSL dictionary without logging in, so that I can explore the app before committing to sign up. | Guest | 🔴 | 3 |
| US-51 | As a **User**, I want to browse VSL signs by category (e.g., Greetings, Numbers, Family), so that I can explore signs by topic. | All | 🔴 | 3 |
| US-52 | As a **User**, I want to search for a specific sign by name, so that I can quickly find what I'm looking for. | All | 🔴 | 3 |
| US-53 | As a **User**, I want to watch a video demonstration of each sign, so that I can see the correct hand movement. | All | 🔴 | 2 |
| US-54 | As a **User**, I want to see a difficulty level for each sign, so that I can choose signs appropriate for my skill level. | All | 🟡 | 1 |
| US-55 | As a **User**, I want a "Practice this sign" button on dictionary entries, so that I can jump straight into AI practice for that sign. | Premium | 🟢 | 3 |

---

## EPIC 6: Monetization (Freemium & Payment)

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-56 | As a **Basic User**, I want to see a clear paywall screen when I try to access premium content, so that I understand what I need to do to unlock it. | Basic | 🔴 | 3 |
| US-57 | As a **Basic User**, I want to view the premium pricing plans (Monthly/Yearly), so that I can choose the option that fits my budget. | Basic | 🔴 | 2 |
| US-58 | As a **Basic User**, I want to pay for Premium using MoMo QR code, so that I can upgrade quickly using my preferred payment method. | Basic | 🔴 | 8 |
| US-59 | As a **Basic User**, I want to pay for Premium using ZaloPay QR code, so that I have an alternative local payment option. | Basic | 🟡 | 5 |
| US-60 | As a **Basic User**, I want my account to be upgraded to Premium immediately after a successful payment, so that I don't have to wait or contact support. | Basic | 🔴 | 5 |
| US-61 | As a **User**, I want to see a payment confirmation screen after a successful transaction, so that I have proof that my payment went through. | Basic | 🔴 | 2 |
| US-62 | As a **User**, I want to see a clear error message if my payment fails, so that I know what went wrong and can try again. | Basic | 🔴 | 2 |
| US-63 | As a **User**, I want to view my subscription status and expiry date on my profile, so that I know when to renew. | Premium | 🟡 | 2 |
| US-64 | As a **User**, I want to view my payment history, so that I can verify my transactions. | Premium | 🟡 | 3 |

---

## EPIC 7: Admin Panel

| ID | User Story | Actor | Priority | Points |
|---|---|---|---|---|
| US-65 | As an **Admin**, I want to create, edit, and delete Units, so that I can manage the top-level curriculum structure. | Admin | 🔴 | 5 |
| US-66 | As an **Admin**, I want to create Chapters inside a Unit and set `is_premium`, so that I can control which content is free vs paid. | Admin | 🔴 | 5 |
| US-67 | As an **Admin**, I want to upload lesson videos to S3 and link them to a Lesson, so that learners can watch video content. | Admin | 🔴 | 8 |
| US-68 | As an **Admin**, I want to create MCQ quiz questions with options and correct answers, so that I can build assessments for each lesson. | Admin | 🔴 | 5 |
| US-69 | As an **Admin**, I want to create AI Quiz questions by specifying a `sign_label`, so that the system knows which sign to check during AI practice. | Admin | 🔴 | 3 |
| US-70 | As an **Admin**, I want to add and edit Dictionary entries with video and category, so that learners have a reference resource. | Admin | 🔴 | 5 |
| US-71 | As an **Admin**, I want to view a list of all registered users, so that I can monitor platform growth. | Admin | 🟡 | 3 |
| US-72 | As an **Admin**, I want to see each user's subscription status, so that I can handle support requests about account access. | Admin | 🟡 | 2 |
| US-73 | As an **Admin**, I want to view all payment transactions with status (PENDING/SUCCESS/FAILED), so that I can reconcile payments and handle failed webhook cases. | Admin | 🔴 | 5 |
| US-74 | As an **Admin**, I want to manually trigger a subscription status update for a specific transaction, so that I can resolve edge cases where webhook delivery failed. | Admin | 🟡 | 5 |
| US-75 | As an **Admin**, I want to view a dashboard with total users, active subscriptions, and daily active learners, so that I can monitor business KPIs. | Admin | 🟡 | 8 |

---

## Story Summary

| Epic | Total Stories | Must Have 🔴 | Should Have 🟡 | Could Have 🟢 |
|---|---|---|---|---|
| Authentication | 8 | 4 | 4 | 0 |
| Learning | 10 | 7 | 3 | 0 |
| Assessment | 14 | 8 | 5 | 1 |
| Gamification | 17 | 7 | 9 | 1 |
| Dictionary | 6 | 4 | 1 | 1 |
| Monetization | 9 | 6 | 3 | 0 |
| Admin | 11 | 7 | 4 | 0 |
| **TOTAL** | **75** | **43** | **29** | **3** |

---

## Sprint Suggestion (4 Tuần)

| Sprint | Focus | US IDs | Story Points |
|---|---|---|---|
| **Sprint 1** | Core Foundation: Auth + Learning skeleton + Admin content management | US-01–18, US-65–70 | ~45 pts |
| **Sprint 2** | Assessment: MCQ Quiz + Timed Test + Payment flow | US-19–26, US-56–64 | ~40 pts |
| **Sprint 3** | AI Quiz + Gamification | US-27–49 | ~45 pts |
| **Sprint 4** | Dictionary + Admin dashboard + Polish + Bug fix | US-50–55, US-71–75 | ~35 pts |
