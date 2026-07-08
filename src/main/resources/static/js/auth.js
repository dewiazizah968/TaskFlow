/* ==========================================================================
   TaskFlow Auth Pages - Shared Behaviour (Login & Register)
   ========================================================================== */

// Accepts standard email formats, e.g. name@gmail.com, name@email.com,
// name@company.co.id, or any other valid domain.
const EMAIL_PATTERN = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

document.addEventListener('DOMContentLoaded', () => {
    setupPasswordToggles();
    setupInlineValidation();
    setupModal();
    setupLoginForm();
    setupRegisterForm();
    setupForgotPassword();
    setupGoogleSetPasswordForm();
    setupResetPasswordForm();
    checkUrlFeedback();
});

/* ------------------------------ Password toggle ------------------------------ */

function setupPasswordToggles() {
    document.querySelectorAll('input[type="password"]').forEach((input) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'input-wrapper';
        input.parentNode.insertBefore(wrapper, input);
        wrapper.appendChild(input);

        const toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'toggle-password';
        toggle.textContent = 'SHOW';
        toggle.setAttribute('aria-label', 'Show password');

        toggle.addEventListener('click', () => {
            const isHidden = input.type === 'password';
            input.type = isHidden ? 'text' : 'password';
            toggle.textContent = isHidden ? 'HIDE' : 'SHOW';
        });

        wrapper.appendChild(toggle);
    });
}

/* ------------------------------ Inline validation ------------------------------ */

function setupInlineValidation() {
    document.querySelectorAll('.form-group').forEach((group) => {
        const input = group.querySelector('input');
        if (!input) return;

        const errorEl = document.createElement('span');
        errorEl.className = 'error-message';
        group.appendChild(errorEl);

        input.addEventListener('blur', () => validateField(input, errorEl));
        input.addEventListener('input', () => {
            if (input.classList.contains('input-error')) {
                validateField(input, errorEl);
            }
        });
    });
}

function validateField(input, errorEl) {
    let message = '';

    if (input.validity.valueMissing) {
        message = 'This field is required.';
    } else if (input.type === 'email' && !EMAIL_PATTERN.test(input.value)) {
        message = 'Please enter a valid email address.';
    } else if (input.type === 'password' && input.value && input.value.length < 6) {
        message = 'Password must be at least 6 characters.';
    }

    errorEl.textContent = message;
    input.classList.toggle('input-error', Boolean(message));
    return message === '';
}

function validateForm(form) {
    let isValid = true;
    form.querySelectorAll('.form-group').forEach((group) => {
        const input = group.querySelector('input');
        const errorEl = group.querySelector('.error-message');
        if (input && errorEl) {
            const fieldValid = validateField(input, errorEl);
            isValid = isValid && fieldValid;
        }
    });
    return isValid;
}

/* ------------------------------ Warning modal ------------------------------ */

function setupModal() {
    if (document.querySelector('[data-warning-modal]')) return;

    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.setAttribute('data-warning-modal', '');
    overlay.innerHTML = `
        <div class="modal-box">
            <div class="modal-icon">!</div>
            <div class="modal-title" data-modal-title>Something went wrong</div>
            <div class="modal-message" data-modal-message></div>
            <button type="button" class="btn btn-primary" data-modal-close>
                <span class="btn-label">OK</span>
            </button>
        </div>
    `;
    document.body.appendChild(overlay);

    overlay.addEventListener('click', (e) => {
        if (e.target === overlay || e.target.closest('[data-modal-close]')) {
            closeModal();
        }
    });
}

function showWarning(message, title = 'Something went wrong') {
    const overlay = document.querySelector('[data-warning-modal]');
    if (!overlay) return;
    overlay.querySelector('[data-modal-title]').textContent = title;
    overlay.querySelector('[data-modal-message]').textContent = message;
    overlay.classList.add('show');
}

function closeModal() {
    const overlay = document.querySelector('[data-warning-modal]');
    if (overlay) overlay.classList.remove('show');
}

/* ------------------------------ Button loading state ------------------------------ */

function setButtonLoading(button, isLoading) {
    if (!button) return;
    if (isLoading) {
        if (!button.querySelector('.spinner')) {
            const label = document.createElement('span');
            label.className = 'btn-label';
            label.textContent = button.textContent.trim();
            const spinner = document.createElement('span');
            spinner.className = 'spinner';
            button.textContent = '';
            button.appendChild(spinner);
            button.appendChild(label);
        }
        button.classList.add('is-loading');
        button.setAttribute('disabled', 'true');
    } else {
        button.classList.remove('is-loading');
        button.removeAttribute('disabled');
    }
}

/* ------------------------------ Login form ------------------------------ */

function setupLoginForm() {
    const form = document.querySelector('#login-form');
    if (!form) return;

    form.addEventListener('submit', (event) => {
        if (!validateForm(form)) {
            event.preventDefault();
            return;
        }
        // Otherwise let the form submit normally to Spring Security's /login.
        // On failure, Spring redirects back to /login?error, handled by
        // checkUrlFeedback() below.
        const submitBtn = form.querySelector('button[type="submit"]');
        setButtonLoading(submitBtn, true);
    });
}

/* ------------------------------ Register form ------------------------------ */

function setupRegisterForm() {
    const form = document.querySelector('#register-form');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!validateForm(form)) {
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        setButtonLoading(submitBtn, true);

        const formData = new FormData(form);
        const payload = {
            name: formData.get('name'),
            email: formData.get('email'),
            password: formData.get('password'),
        };

        try {
            const response = await fetch(form.action, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });

            if (response.ok) {
                window.location.href = '/login?registered';
                return;
            }

            let message = 'Registration failed. Please check your email and password.';
            try {
                const data = await response.json();
                if (data && data.message) message = data.message;
            } catch (e) {
                // ignore non-JSON error bodies
            }
            showWarning(message, 'Registration failed');
        } catch (err) {
            showWarning('Unable to reach the server. Please try again.', 'Network error');
        } finally {
            setButtonLoading(submitBtn, false);
        }
    });
}

/* ------------------------------ Google - set password after verification ------------------------------ */

function setupGoogleSetPasswordForm() {
    const form = document.querySelector('#google-set-password-form');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!validateForm(form)) {
            return;
        }

        const password = form.querySelector('#gsp-password').value;
        const confirmPassword = form.querySelector('#gsp-confirm-password').value;

        if (password !== confirmPassword) {
            showWarning('Passwords do not match. Please try again.', 'Password mismatch');
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        setButtonLoading(submitBtn, true);

        try {
            const response = await fetch('/api/auth/register-google', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ password, confirmPassword }),
            });

            if (response.ok) {
                window.location.href = '/login?registered';
                return;
            }

            let message = 'Could not finish creating your account. Please try again.';
            try {
                const data = await response.json();
                if (data && data.message) message = data.message;
            } catch (e) {
                // ignore non-JSON error bodies
            }
            showWarning(message, 'Something went wrong');
        } catch (err) {
            showWarning('Unable to reach the server. Please try again.', 'Network error');
        } finally {
            setButtonLoading(submitBtn, false);
        }
    });
}

/* ------------------------------ Forgot password ------------------------------ */

function setupForgotPassword() {
    const link = document.querySelector('#forgot-password-link');
    const overlay = document.querySelector('#forgot-password-overlay');
    const form = document.querySelector('#forgot-password-form');
    const cancelBtn = document.querySelector('#forgot-password-cancel');
    const resultBox = document.querySelector('#forgot-password-result');

    if (!link || !overlay || !form) return;

    const openModal = (event) => {
        event.preventDefault();
        resultBox.classList.remove('show', 'success', 'error');
        resultBox.textContent = '';
        form.reset();
        form.querySelectorAll('.error-message').forEach((el) => (el.textContent = ''));
        form.querySelectorAll('input').forEach((el) => el.classList.remove('input-error'));
        overlay.classList.add('show');
    };

    const closeForgotModal = () => {
        overlay.classList.remove('show');
    };

    link.addEventListener('click', openModal);

    if (cancelBtn) {
        cancelBtn.addEventListener('click', closeForgotModal);
    }

    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) closeForgotModal();
    });

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!validateForm(form)) {
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        setButtonLoading(submitBtn, true);
        resultBox.classList.remove('show', 'success', 'error');

        const emailInput = form.querySelector('#forgot-email');
        const payload = { email: emailInput.value.trim() };

        try {
            const response = await fetch('/api/auth/forgot-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });

            let data = {};
            try {
                data = await response.json();
            } catch (e) {
                // ignore non-JSON body
            }

            if (response.ok) {
                resultBox.textContent = data.message || "If that email is registered, we've sent a password reset link to it.";
                resultBox.classList.add('show', 'success');
                form.querySelector('button[type="submit"]').setAttribute('disabled', 'true');
            } else {
                resultBox.textContent = data.message || 'Please enter a valid email address.';
                resultBox.classList.add('show', 'error');
            }
        } catch (err) {
            resultBox.textContent = 'Unable to reach the server. Please try again.';
            resultBox.classList.add('show', 'error');
        } finally {
            setButtonLoading(submitBtn, false);
        }
    });
}

/* ------------------------------ Reset password (from emailed link) ------------------------------ */

function setupResetPasswordForm() {
    const form = document.querySelector('#reset-password-form');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!validateForm(form)) {
            return;
        }

        const token = document.querySelector('#reset-token').value;
        const password = form.querySelector('#rp-password').value;
        const confirmPassword = form.querySelector('#rp-confirm-password').value;

        if (password !== confirmPassword) {
            showWarning('Passwords do not match. Please try again.', 'Password mismatch');
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        setButtonLoading(submitBtn, true);

        try {
            const response = await fetch('/api/auth/reset-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    token: token,
                    newPassword: password,
                    confirmNewPassword: confirmPassword,
                }),
            });

            if (response.ok) {
                window.location.href = '/login?reset=success';
                return;
            }

            let message = 'Could not reset your password. Please request a new link.';
            try {
                const data = await response.json();
                if (data && data.message) message = data.message;
            } catch (e) {
                // ignore non-JSON error bodies
            }
            showWarning(message, 'Something went wrong');
        } catch (err) {
            showWarning('Unable to reach the server. Please try again.', 'Network error');
        } finally {
            setButtonLoading(submitBtn, false);
        }
    });
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

/* ------------------------------ URL feedback (login errors, etc.) ------------------------------ */

function checkUrlFeedback() {
    const params = new URLSearchParams(window.location.search);

    if (params.get('error') === 'google') {
        showWarning('Google login failed or was cancelled. Please try again.', 'Login failed');
    } else if (params.get('error') === 'google_unverified') {
        showWarning('We could not verify that Google account. Please try again.', 'Login failed');
    } else if (params.has('error')) {
        showWarning('Incorrect email or password. Please try again.', 'Login failed');
    }

    if (params.has('registered')) {
        showWarning('Your account has been created. You can log in now.', 'Registration successful');
    }

    if (params.get('reset') === 'success') {
        showWarning('Your password has been reset. You can now log in with your new password.', 'Password reset');
    }
}