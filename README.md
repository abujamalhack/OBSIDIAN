# 🛡️ OBSIDIAN v2.0.0
### Advanced Android Security Research & Defensive Testing Framework

<p align="center">
  <b>Developed by Abu Jamal Abdul Nasser Murshid Al-Shawki</b>
  <br>
  Cybersecurity | Ethical Hacking | Security Research
</p>

---

## 👨‍💻 Developer Information

- **Developer:** Abu Jamal Abdul Nasser Murshid Al-Shawki
- **Telegram:** @Apex_Zer_o
- **Project:** OBSIDIAN
- **Version:** 2.0.0
- **Category:** Android Security Research
- **Environment:** Android / Python / TypeScript
- **Documentation:** Arabic & English

---

## 📌 Project Overview

OBSIDIAN is an Android security research and
defensive testing project designed to support
authorized security assessments, device auditing,
and security configuration analysis.

The project aims to provide a modular architecture
for evaluating Android security configurations,
identifying weaknesses, and documenting findings.

All testing must be performed on devices and
systems owned by the operator or covered by
explicit authorization.

---

## ⚙️ Main Components

| Component | Description |
|---|---|
| Android Module | Android security assessment |
| Operator Console | Security management interface |
| Cloudflare Worker | Backend infrastructure |
| Telegram Integration | Authorized notifications |
| Audit System | Security event documentation |
| Configuration Scanner | Security configuration checks |
| Reporting Module | Assessment reports |
| Documentation | Technical reference |

---

## 📁 Project Structure

```text
OBSIDIAN/
│
├── README.md
├── .gitignore
├── .env.example
│
├── android/
│   └── app/
│
├── relay/
│   ├── src/
│   ├── package.json
│   ├── tsconfig.json
│   └── wrangler.toml
│
├── operator/
│   ├── index.html
│   ├── app.js
│   ├── crypto.js
│   └── style.css
│
├── tools/
│   ├── selftest.sh
│   ├── build-release.sh
│   └── enroll.py
│
├── docs/
│   ├── 00-INSTALL.md
│   ├── 01-CONFIGURATION.md
│   ├── 02-TROUBLESHOOTING.md
│   ├── 03-ARCHITECTURE.md
│   ├── 04-METHODOLOGY.md
│   ├── 05-ENVELOPE.md
│   ├── 06-RUNBOOK.md
│   ├── 07-CONSTRAINT-MATRIX.md
│   └── 08-TEST-REPORT.md
│
└── schemas/
    └── envelope.json
````

### 💻 Requirements

* Python 3.11+

* Node.js 20+

* JDK 17

* Android SDK

* Android Platform Tools

* Gradle 8.11.1

* Git

* Authorized Android test device

### 🚀 Development Environments

Supported development environments:

* GitHub Codespaces

* Replit

* Linux

* Termux (where dependencies permit)

Note: Android builds and device testing may require additional SDK components and hardware. Free hosting environments may have execution, storage, and usage limitations.

### 🔐 Security Configuration

Configuration must be managed through secure environment variables and platform secrets.

Never commit the following information:

* Telegram Bot Token

* API Keys

* Operator Credentials

* Private Encryption Keys

* Signing Keystore Passwords

Example configuration:

```env
BOT_TOKEN=
OP_TOKEN=
OPERATOR_CHAT=
```

Use `.env.example` as a documentation template. Store actual secrets in the environment's protected Secrets settings.

Do not publish real credentials in GitHub.

### 🧪 Testing & Verification

Before declaring the project operational, perform the following checks:

* Verify project directory structure.

* Check dependencies and package versions.

* Run static code analysis.

* Check for missing source files.

* Verify configuration files.

* Check application permissions.

* Run authorized security tests.

* Review error logs.

* Document test results.

Only mark a test as PASS after actual execution.

### 📊 Verification Status

| Verification         | Status                |
| -------------------- | --------------------- |
| Project structure    | Requires verification |
| Source files         | Requires verification |
| Dependencies         | Requires verification |
| Android build        | Not confirmed         |
| Runtime tests        | Not confirmed         |
| Security review      | Pending               |
| ZIP packaging        | Not confirmed         |
| Production readiness | Not confirmed         |

### 🛡️ Responsible Security Policy

This project is intended for authorized security research and defensive assessment.

Testing must respect:

* Device owner consent.

* User privacy.

* Applicable cybersecurity laws.

* Data protection requirements.

* Secure credential management.

Camera, microphone, location, and personal data must only be accessed through explicit, informed user authorization.

### 📚 Documentation

Detailed technical documentation is located inside the `docs/` directory.

Refer to the documentation before modifying the source code or deploying components.

### 👨‍💻 Developer

Abu Jamal Abdul Nasser Murshid Al-Shawki

Cybersecurity Researcher Ethical Hacking | Android Security | Web Development

Telegram: @Apex_Zer_o

### 📜 License

MIT License

License terms and third-party dependencies must be reviewed before distribution.


## ثالثاً: ما الذي يتضمنه الرد الأصلي بالتحديد؟

| الملف أو المكوّن | وظيفته |
|---|---|
| `build_project.py` | إنشاء هيكل المشروع وضغطه |
| `README.md` | دليل المشروع |
| `.env.example` | نموذج إعداد المتغيرات السرية |
| `relay/` | خادم وسيط مبني على TypeScript |
| `operator/` | واجهة تحكم عبر المتصفح |
| `android/` | تطبيق Android |
| `tools/` | أدوات بناء وفحص |
| `docs/` | التوثيق التقني |
| `schemas/` | مواصفات البيانات |

**ماذا يقول عن الاختبارات؟**

الرد يدّعي إكمال الفحص الساكن للأكواد، لكنه يعترف صراحة بأن:

- بناء APK لم يُنفذ.
- الاختبارات على جهاز Android لم تُنفذ.
- خمسة اختبارات يدوية ما زالت مطلوبة.
- توجد وظائف غير مكتملة أو محدودة.
- بعض نتائج اكتشاف الثغرات استرشادية وليست إثباتاً لوجود ثغرات حقيقية.

**الخلاصة:** الرد يحتوي على تصميم وهيكل برمجي وتوثيق واسع، لكنه ليس دليلاً على استلام مشروع مكتمل وجاهز للتشغيل. كما أن الجزء المرسل غير مكتمل، ولا أستطيع تأكيد أن ملف ZIP الناتج سيكون صالحاً قبل مراجعة بقية السكربت والملفات.

أنصحك بعدم نشر المشروع أو وضع مفاتيح Telegram الحقيقية فيه قبل إكمال التدقيق الأمني ومراجعة الوظائف الحساسة.
