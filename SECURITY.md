# Security Policy

## Reporting a vulnerability

Do not disclose suspected vulnerabilities in a public issue.

Use GitHub's private vulnerability reporting from the repository's **Security** tab. Include the affected component, reproduction steps, impact, and any suggested mitigation. The repository owner will review the report and coordinate a fix before public disclosure.

## Automated checks

| Check | Runs on | Blocking threshold |
| --- | --- | --- |
| Secret Scan (Gitleaks) | Every push and pull request | Any detected secret fails the check. The CLI version and release checksum are pinned; matches are redacted and no PR comments or artifacts are produced. |
| CodeQL (`security-extended`) | Every push, pull request to `main`, and weekly | High or critical security findings (security severity `>= 7.0`) fail the check. Lower-severity findings still require review and remediation or a documented exception before the change is considered ready. |
| Dependency Review | Pull requests to `main` | Newly introduced vulnerabilities of moderate severity or higher fail the check. |
| Android Lint | CI pushes and pull requests to `main` | Lint errors fail CI. This is part of the build workflow, not duplicated in the security workflows. |

These workflows only analyze the repository; they do not publish application builds or deploy the app. CodeQL uploads its analysis to GitHub Code Security so findings can be reviewed there.

## False positives and exceptions

Do not suppress a finding just to make a check pass. First verify whether it is a real issue and, for a secret, rotate/revoke it even if it was committed accidentally. A false positive or accepted risk may be suppressed only through a narrowly scoped Gitleaks fingerprint in `.gitleaksignore` or a CodeQL configuration, and must be recorded in the pull request with:

- the finding/rule and affected path, without reproducing any secret value;
- the reason it is a false positive or accepted risk;
- the mitigation or compensating control;
- an owner and an expiration date.

The exception must be removed or re-evaluated by its expiration date. There are no approved exceptions at present.
