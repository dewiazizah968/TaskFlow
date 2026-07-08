# TaskFlow

TaskFlow is a web-based project and task management application built with **Spring Boot**. It lets a user create projects, invite members, break work down into tasks with statuses/priorities/deadlines, attach files or links to tasks, chat inside a project, get notifications, view a calendar of deadlines, and export project/task data as PDF reports.

This is a personal / portfolio project built to practice a full-stack Spring Boot setup: server-rendered pages with Thymeleaf, a REST API layer consumed by JavaScript for dynamic parts of the UI, Spring Security (including Google OAuth2 login), and PostgreSQL for persistence.

## Features

- **Authentication**
  - Classic email/password registration and login.
  - "Continue with Google" (OAuth2) login. New Google users are routed through a set-password step so they also have a normal password for the account.
  - Secure, single-use, time-limited password reset tokens sent by email (via SMTP/Brevo).
- **Projects & members**
  - Create, edit, and delete projects.
  - Add/remove members to a project.
  - Per-project dashboard with progress summary.
- **Tasks**
  - Create, edit, delete tasks with status (`TODO`, `IN_PROGRESS`, `DONE`), priority (`LOW`, `MEDIUM`, `HIGH`), and deadline.
  - Filter tasks by status, priority, or assigned user.
  - Task history/audit trail of changes.
  - Attach files (uploaded to disk) or external links to a task.
- **Collaboration**
  - Lightweight chat per project (also usable as a personal notes stream on a project with no other members).
  - Notifications (e.g. upcoming deadlines) with a "mark as read" endpoint.
- **Calendar** view of task deadlines.
- **Search** across the app's data.
- **Reports**
  - Per-project PDF report.
  - Full personal data export (PDF) for a user.
- **Profile**
  - Update profile info, change password, upload a profile picture.
- **Theme** toggle (light/dark), persisted per session/user.

## Tech stack

| Layer            | Technology |
|-------------------|------------|
| Language           | Java 17 |
| Framework          | Spring Boot 3.5.14 |
| Security           | Spring Security (form login + OAuth2 login), `thymeleaf-extras-springsecurity6` |
| Web / templating   | Spring Web MVC, Thymeleaf, vanilla JS/CSS for interactive parts |
| Persistence        | Spring Data JPA + PostgreSQL |
| Validation         | Spring Boot Validation (Jakarta Bean Validation) |
| Email              | Spring Boot Mail (JavaMailSender) over SMTP, using Brevo as the SMTP relay |
| PDF generation      | OpenPDF 1.3.39 (`com.lowagie.text`) |
| Build tool          | Maven |

## Project structure

```
src/main/java/com/taskflow/
├── config/         # Spring Security configuration
├── controller/      # MVC page controllers + REST (@RestController-style) API controllers
├── dto/             # Request/response payloads for the API
├── entity/           # JPA entities (User, Project, Task, ProjectMember, ProjectMessage, TaskFile, Notification, TaskHistory, PasswordResetToken, ...)
├── exception/        # Centralized exception handling
├── repository/       # Spring Data JPA repositories
├── security/          # CustomUserDetailsService, CustomUserDetails, OAuth2LoginSuccessHandler
├── service/           # Business logic (auth, projects, tasks, chat, notifications, email, PDF reports, file uploads, ...)
└── util/              # Shared helpers (PDF report styling)

src/main/resources/
├── static/            # CSS, JS, images
├── templates/          # Thymeleaf pages + reusable fragments (sidebar, modals, footer, ...)
└── fonts/               # Fonts embedded into generated PDF reports
```

## How it works

### Authentication flow

- **Email/password**: `POST /api/auth/register` creates a user with a BCrypt-hashed password; `POST /api/auth/login` and Spring Security's form login handle sign-in.
- **Google OAuth2**: Spring Security's `oauth2Login` hands control to `OAuth2LoginSuccessHandler` once Google confirms the user's identity.
  - If a user with that email already exists, their account is linked to the Google ID (if not already) and the session proceeds straight to `/dashboard`.
  - If it's a brand-new Google user, the handler clears the OAuth2-authenticated session (so the person can't wander into the app half-authenticated), stashes the verified Google profile in a fresh session, and redirects to `/register/set-password` where the person chooses a password to finish creating their local account (`POST /api/auth/register-google`).
- **Forgot password**: `POST /api/auth/forgot-password` generates a single-use UUID token with an expiry (`PasswordResetToken`), and emails a reset link (`EmailService` via SMTP). `POST /api/auth/reset-password` validates the token (must exist, be unused, and not expired) before letting the user set a new password; the token is then marked used so the link can't be replayed.
- Public/unauthenticated routes (`/`, `/login`, `/register`, static assets, OAuth2 endpoints, etc.) are configured in `SecurityConfig`; everything else requires an authenticated session.

### Application flow

1. A logged-in user lands on `/dashboard`, which summarizes their projects and tasks.
2. From `/projects`, they can create a project, add members, and open a project's detail page.
3. Inside a project, tasks can be created/edited/moved through statuses, with files/links attached and a history trail kept per task.
4. Project members can chat in real time-ish via polling endpoints (`/api/projects/{id}/chat/messages` and `/messages/after/{id}`).
5. Notifications are generated (e.g. for approaching deadlines) and surfaced via `/api/notifications/me`.
6. `/calendar` renders all of a user's task deadlines.
7. Reports can be downloaded as PDF, either scoped to one project (`/api/projects/{id}/report`) or as a full personal data export from the dashboard (`/dashboard/export`).

## Getting started (running locally)

### Prerequisites

- JDK 17
- Maven (or just use the bundled `./mvnw` / `mvnw.cmd`)
- PostgreSQL running locally (or accessible remotely)
- A Google Cloud OAuth2 Client ID/Secret (only needed if you want to test "Continue with Google")
- A Brevo (or any other SMTP) account (only needed if you want to test the forgot-password email flow)

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/taskflow.git
cd taskflow
```

### 2. Create your local configuration

The real `application.properties` is **not** committed to this repository (it holds database credentials, OAuth2 secrets, and SMTP credentials). A sanitized template is provided at [`application.properties.example`](./application.properties.example) — copy it into place and fill in your own values:

```bash
cp application.properties.example src/main/resources/application.properties
```

```properties
spring.application.name=taskflow

# --- PostgreSQL ---
spring.datasource.url=jdbc:postgresql://localhost:5432/taskflow_db
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.thymeleaf.cache=false

# --- File uploads ---
file.upload-dir=uploads/attachments
profile.upload-dir=uploads/profiles
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# --- Google OAuth2 login ("Continue with Google") ---
# Create credentials at https://console.cloud.google.com/apis/credentials
# Authorized redirect URI: http://localhost:8080/login/oauth2/code/google
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.provider.google.user-name-attribute=email

# --- Password reset emails ("Forgot password") ---
app.base-url=http://localhost:8080
app.password-reset.expiry-minutes=30

# --- Brevo SMTP relay (used to actually send emails) ---
spring.mail.host=smtp-relay.brevo.com
spring.mail.port=587
spring.mail.username=YOUR_BREVO_SMTP_LOGIN
spring.mail.password=YOUR_BREVO_SMTP_KEY
app.mail.sender-name=TaskFlow
app.mail.from-address=your-sender@yourdomain.com
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

> Adjust values to match your own database, Google Cloud OAuth2 credentials, and Brevo (or other SMTP provider) account. `spring.mail.username` is the SMTP login Brevo gives you, while `app.mail.from-address` is the address recipients actually see — keeping them separate is why both properties exist. None of the real `application.properties` should ever be pushed to GitHub; it's excluded via `.gitignore`.

### 3. Create the database

```sql
CREATE DATABASE taskflow;
```

Tables are created/updated automatically on startup via `spring.jpa.hibernate.ddl-auto=update`.

### 4. Run the app

Using the Maven wrapper:

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Or build a jar and run it:

```bash
./mvnw clean package
java -jar target/taskflow-*.jar
```

### 5. Open it in a browser

```
http://localhost:8080
```

Register a new account (or sign in with Google, if configured) and start creating projects.

## Notes

- File uploads are written to two separate folders — task attachments to `file.upload-dir` and profile pictures to `profile.upload-dir` — both excluded from version control since they're runtime/user-generated content rather than source.
- PDF reports are generated with OpenPDF and embed the DejaVu Sans fonts bundled under `src/main/resources/fonts` so they render correctly regardless of the OS running the app.
