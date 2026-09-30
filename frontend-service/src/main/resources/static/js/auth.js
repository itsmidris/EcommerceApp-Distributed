const AUTH_API = {
    login: "/api/auth/login",
    register: "/api/users",
    forgotPassword: "/api/auth/forgot-password",
    verifyOtp: "/api/auth/verify-otp",
    resetPassword: "/api/auth/reset-password"
};

document.addEventListener("DOMContentLoaded", () => {
    setupPasswordToggles();
    setupLogin();
    setupRegistration();
    setupForgotPassword();
    setupResetPassword();
    setupOtpInputs();
    setupLogout();
    showExpiredMessage();
    showAccessDeniedMessage();
});

function setupPasswordToggles() {
    document.querySelectorAll("[data-password-target]").forEach(button => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.passwordTarget);
            const icon = button.querySelector("i");

            if (!input) return;

            const visible = input.type === "text";
            input.type = visible ? "password" : "text";

            if (icon) {
                icon.classList.toggle("bi-eye", visible);
                icon.classList.toggle("bi-eye-slash", !visible);
            }

            button.setAttribute("aria-label", visible ? "Show password" : "Hide password");
        });
    });
}

function setupLogin() {
    const form = document.getElementById("loginForm");
    if (!form) return;

    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearAlert();

        const email = value("email");
        const password = document.getElementById("password")?.value || "";

        if (!isValidEmail(email)) return showAlert("Enter a valid email address.", "danger");
        if (password.length < 8) return showAlert("Password must contain at least 8 characters.", "danger");

        setLoading("loginBtn", "loginSpinner", true);

        try {
            const { response, data } = await SellzonAPI.json(AUTH_API.login, {
                method: "POST",
                body: JSON.stringify({ email, password })
            });

            if (!response.ok) throw new Error(getBackendMessage(data, "Invalid email or password."));

            SellzonAPI.saveAuth(data);

            const expected = SellzonAPI.getExpectedRole();
            const actual = SellzonAPI.normalizeRole(data.role);

            if (actual !== expected) {
                SellzonAPI.clearAuth();
                showAlert(`This account is not a ${portalRoleLabel(expected)} account.`, "danger");
                return;
            }

            const params = new URLSearchParams(window.location.search);
            const redirect = params.get("redirect");

            if (expected === "CUSTOMER" && redirect && redirect.startsWith("/") && !redirect.startsWith("//")) {
                window.location.replace(redirect);
            } else {
                window.location.replace(SellzonAPI.getHomePath());
            }
        } catch (error) {
            showAlert(error.message, "danger");
        } finally {
            setLoading("loginBtn", "loginSpinner", false);
        }
    });
}

function setupRegistration() {
    const form = document.getElementById("registerForm");
    if (!form) return;

    const role = form.dataset.registrationRole || "CUSTOMER";

    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearAlert();

        if (role !== "CUSTOMER") {
            showAlert("Seller onboarding is not enabled yet. We will connect the partner registration flow to the Seller module.", "warning");
            return;
        }

        const firstName = value("firstName");
        const lastName = value("lastName");
        const email = value("registerEmail");
        const phoneNumber = value("phoneNumber");
        const password = document.getElementById("registerPassword")?.value || "";
        const confirmPassword = document.getElementById("confirmPassword")?.value || "";

        if (!firstName || !lastName || !email || !phoneNumber || !password || !confirmPassword)
            return showAlert("Please fill in all required fields.", "danger");
        if (firstName.length > 50 || lastName.length > 50)
            return showAlert("First and last names must not exceed 50 characters.", "danger");
        if (!isValidEmail(email))
            return showAlert("Enter a valid email address.", "danger");
        if (email.length > 100)
            return showAlert("Email must not exceed 100 characters.", "danger");
        if (!/^[6-9]\d{9}$/.test(phoneNumber))
            return showAlert("Enter a valid 10-digit Indian phone number.", "danger");
        if (password.length < 8 || password.length > 100)
            return showAlert("Password must be between 8 and 100 characters.", "danger");
        if (password !== confirmPassword)
            return showAlert("Passwords do not match.", "danger");

        setLoading("registerBtn", "registerSpinner", true);

        try {
            const { response, data } = await SellzonAPI.json(AUTH_API.register, {
                method: "POST",
                body: JSON.stringify({ firstName, lastName, email, password, phoneNumber, role: "CUSTOMER" })
            });

            if (!response.ok) throw new Error(getBackendMessage(data, "Unable to create account."));

            showAlert("Account created successfully. Redirecting to login...", "success");
            form.reset();
            setTimeout(() => window.location.href = SellzonAPI.getLoginPath(), 1200);
        } catch (error) {
            showAlert(error.message, "danger");
        } finally {
            setLoading("registerBtn", "registerSpinner", false);
        }
    });
}

function setupForgotPassword() {
    const form = document.getElementById("forgotPasswordForm");
    if (!form) return;

    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearAlert();

        const email = value("forgotEmail");

        if (!isValidEmail(email)) return showAlert("Enter a valid email address.", "danger");

        setLoading("forgotBtn", "forgotSpinner", true);

        try {
            const { response, data } = await SellzonAPI.json(AUTH_API.forgotPassword, {
                method: "POST",
                body: JSON.stringify({ email })
            });

            if (!response.ok) throw new Error(getBackendMessage(data, "Unable to send OTP."));

            sessionStorage.setItem("sellzonResetEmail", email);
            window.location.href = SellzonAPI.getResetPasswordPath();
        } catch (error) {
            showAlert(error.message, "danger");
        } finally {
            setLoading("forgotBtn", "forgotSpinner", false);
        }
    });
}

function setupOtpInputs() {
    const boxes = document.querySelectorAll(".otp-box");
    if (!boxes.length) return;

    boxes.forEach((box, index) => {
        box.addEventListener("input", () => {
            box.value = box.value.replace(/\D/g, "").slice(0, 1);
            if (box.value && index < boxes.length - 1) boxes[index + 1].focus();
        });

        box.addEventListener("keydown", event => {
            if (event.key === "Backspace" && !box.value && index > 0) boxes[index - 1].focus();
            if (event.key === "ArrowLeft" && index > 0) boxes[index - 1].focus();
            if (event.key === "ArrowRight" && index < boxes.length - 1) boxes[index + 1].focus();
        });

        box.addEventListener("paste", event => {
            event.preventDefault();
            const pasted = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
            pasted.split("").forEach((digit, i) => {
                if (boxes[i]) boxes[i].value = digit;
            });
            if (pasted.length) boxes[Math.min(pasted.length, boxes.length) - 1].focus();
        });
    });
}

function setupResetPassword() {
    const otpSection = document.getElementById("otpSection");
    if (!otpSection) return;

    if (!sessionStorage.getItem("sellzonResetEmail")) {
        showAlert("Reset session expired. Please request a new OTP.", "warning");
        setTimeout(() => {
            window.location.href = SellzonAPI.getForgotPasswordPath();
        }, 1500);
        return;
    }

    document.getElementById("verifyOtpBtn")?.addEventListener("click", verifyOtp);
    document.getElementById("resetPasswordBtn")?.addEventListener("click", resetPassword);
}

async function verifyOtp() {
    clearAlert();

    const email = sessionStorage.getItem("sellzonResetEmail");
    const otp = [...document.querySelectorAll(".otp-box")].map(b => b.value).join("");

    if (!email) return showAlert("Reset session expired. Please request a new OTP.", "danger");
    if (!/^\d{6}$/.test(otp)) return showAlert("Please enter the complete 6-digit OTP.", "danger");

    setLoading("verifyOtpBtn", "verifyOtpSpinner", true);

    try {
        const { response, data } = await SellzonAPI.json(AUTH_API.verifyOtp, {
            method: "POST",
            body: JSON.stringify({ email, otp })
        });

        if (!response.ok) throw new Error(getBackendMessage(data, "Invalid OTP."));
        if (!data?.resetToken) throw new Error("OTP verified, but reset token was not returned.");

        sessionStorage.setItem("sellzonResetToken", data.resetToken);
        document.getElementById("otpSection").classList.add("d-none");
        document.getElementById("newPasswordSection").classList.remove("d-none");
        showAlert("OTP verified. Create your new password.", "success");
    } catch (error) {
        showAlert(error.message, "danger");
    } finally {
        setLoading("verifyOtpBtn", "verifyOtpSpinner", false);
    }
}

async function resetPassword() {
    clearAlert();

    const resetToken = sessionStorage.getItem("sellzonResetToken");
    const newPassword = document.getElementById("newPassword")?.value || "";
    const confirmPassword = document.getElementById("resetConfirmPassword")?.value || "";

    if (!resetToken) return showAlert("Reset session expired. Please request a new OTP.", "danger");
    if (newPassword.length < 8 || newPassword.length > 100)
        return showAlert("Password must be between 8 and 100 characters.", "danger");
    if (newPassword !== confirmPassword)
        return showAlert("Passwords do not match.", "danger");

    setLoading("resetPasswordBtn", "resetPasswordSpinner", true);

    try {
        const { response, data } = await SellzonAPI.json(AUTH_API.resetPassword, {
            method: "POST",
            body: JSON.stringify({ resetToken, newPassword })
        });

        if (!response.ok) throw new Error(getBackendMessage(data, "Unable to reset password."));

        sessionStorage.removeItem("sellzonResetEmail");
        sessionStorage.removeItem("sellzonResetToken");

        showAlert("Password reset successfully. Redirecting to login...", "success");
        setTimeout(() => window.location.href = SellzonAPI.getLoginPath(), 1200);
    } catch (error) {
        showAlert(error.message, "danger");
    } finally {
        setLoading("resetPasswordBtn", "resetPasswordSpinner", false);
    }
}

function setupLogout() {
    document
        .querySelectorAll("[data-logout]")
        .forEach(button => {
            button.addEventListener("click", async event => {
                event.preventDefault();
                button.disabled = true;
                await SellzonAPI.logout();
                window.location.replace(SellzonAPI.getLoginPath());
            });
        });
}

function value(id) {
    return document.getElementById(id)?.value.trim() || "";
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function getBackendMessage(data, fallback) {
    if (!data) return fallback;
    if (Array.isArray(data.errors) && data.errors.length) return data.errors.join(" | ");
    return data.message || fallback;
}

function showAlert(message, type) {
    const alert = document.getElementById("authAlert");
    if (!alert) return;
    alert.className = `alert alert-${type}`;
    alert.textContent = message;
}

function clearAlert() {
    const alert = document.getElementById("authAlert");
    if (!alert) return;
    alert.className = "alert d-none";
    alert.textContent = "";
}

function setLoading(buttonId, spinnerId, loading) {
    const button = document.getElementById(buttonId);
    const spinner = document.getElementById(spinnerId);
    if (button) button.disabled = loading;
    if (spinner) spinner.classList.toggle("d-none", !loading);
}

function showExpiredMessage() {
    if (new URLSearchParams(location.search).get("expired") === "true")
        showAlert("Your session has expired. Please login again.", "warning");
}

function showAccessDeniedMessage() {
    if (new URLSearchParams(location.search).get("access") === "denied")
        showAlert("You do not have access to this portal.", "danger");
}

function portalRoleLabel(role) {
    return { CUSTOMER: "customer", SELLER: "seller", ADMIN: "administrator" }[role] || "required";
}