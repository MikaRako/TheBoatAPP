<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Boat Management - Join the Fleet</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
</head>
<body>
<div class="login-page">

    <div class="brand-header">
        <div class="brand-logo">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="white" width="28" height="28">
                <path d="M20 21c-1.39 0-2.78-.47-4-1.32-2.44 1.71-5.56 1.71-8 0C6.78 20.53 5.39 21 4 21H2v2h2c1.38 0 2.74-.35 4-.99 2.52 1.29 5.48 1.29 8 0 1.26.65 2.62.99 4 .99h2v-2h-2zM3.95 19H4c1.6 0 3.02-.88 4-2 .98 1.12 2.4 2 4 2s3.02-.88 4-2c.98 1.12 2.4 2 4 2h.05l1.89-6.68c.08-.26.06-.54-.06-.78s-.34-.42-.6-.5L20 10.62V6c0-1.1-.9-2-2-2h-3V1H9v3H6c-1.1 0-2 .9-2 2v4.62l-1.29.42c-.26.08-.48.26-.6.5s-.14.52-.06.78L3.95 19zM6 6h12v3.97L12 8 6 9.97V6z"/>
            </svg>
        </div>
        <h1 class="brand-name">Boat Management</h1>
    </div>

    <div class="login-card">
        <h2 class="card-title">Join the Fleet</h2>
        <p class="card-subtitle">Create your account to access fleet management</p>

        <#if message?has_content>
        <div class="alert alert-${message.type}">
            <span>${kcSanitize(message.summary)?no_esc}</span>
        </div>
        </#if>

        <form action="${url.registrationAction}" method="post">

            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="firstName">First Name</label>
                    <input
                        type="text"
                        id="firstName"
                        name="firstName"
                        class="form-input-plain <#if messagesPerField.existsError('firstName')>input-error</#if>"
                        value=""
                        placeholder="First name"
                        autofocus
                        autocomplete="given-name"
                    />
                    <#if messagesPerField.existsError('firstName')>
                    <span class="field-error">${kcSanitize(messagesPerField.get('firstName'))?no_esc}</span>
                    </#if>
                </div>
                <div class="form-group">
                    <label class="form-label" for="lastName">Last Name</label>
                    <input
                        type="text"
                        id="lastName"
                        name="lastName"
                        class="form-input-plain <#if messagesPerField.existsError('lastName')>input-error</#if>"
                        value=""
                        placeholder="Last name"
                        autocomplete="family-name"
                    />
                    <#if messagesPerField.existsError('lastName')>
                    <span class="field-error">${kcSanitize(messagesPerField.get('lastName'))?no_esc}</span>
                    </#if>
                </div>
            </div>

            <div class="form-group">
                <label class="form-label" for="email">Email</label>
                <input
                    type="email"
                    id="email"
                    name="email"
                    class="form-input-plain <#if messagesPerField.existsError('email')>input-error</#if>"
                    value=""
                    placeholder="your@email.com"
                    autocomplete="email"
                />
                <#if messagesPerField.existsError('email')>
                <span class="field-error">${kcSanitize(messagesPerField.get('email'))?no_esc}</span>
                </#if>
            </div>

            <#if !realm.registrationEmailAsUsername>
            <div class="form-group">
                <label class="form-label" for="username">Username</label>
                <input
                    type="text"
                    id="username"
                    name="username"
                    class="form-input-plain <#if messagesPerField.existsError('username')>input-error</#if>"
                    value=""
                    placeholder="Choose a username"
                    autocomplete="username"
                />
                <#if messagesPerField.existsError('username')>
                <span class="field-error">${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
                </#if>
            </div>
            </#if>

            <div class="form-group">
                <label class="form-label" for="password">Password</label>
                <input
                    type="password"
                    id="password"
                    name="password"
                    class="form-input-plain <#if messagesPerField.existsError('password','password-confirm')>input-error</#if>"
                    placeholder="Create a password"
                    autocomplete="new-password"
                />
                <#if messagesPerField.existsError('password')>
                <span class="field-error">${kcSanitize(messagesPerField.get('password'))?no_esc}</span>
                </#if>
            </div>

            <div class="form-group">
                <label class="form-label" for="password-confirm">Confirm Password</label>
                <input
                    type="password"
                    id="password-confirm"
                    name="password-confirm"
                    class="form-input-plain <#if messagesPerField.existsError('password-confirm')>input-error</#if>"
                    placeholder="Confirm your password"
                    autocomplete="new-password"
                />
                <#if messagesPerField.existsError('password-confirm')>
                <span class="field-error">${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</span>
                </#if>
            </div>

            <button type="submit" class="btn-login">JOIN THE FLEET</button>
        </form>
    </div>

    <div class="login-footer">
        <span class="footer-text">Already have an account? <a href="${url.loginUrl}" class="btn-request">Sign in</a></span>
    </div>

</div>
<script>
    // Save field values to sessionStorage just before the form is submitted
    document.querySelector('form').addEventListener('submit', function () {
        ['firstName', 'lastName', 'email', 'username'].forEach(function (id) {
            var el = document.getElementById(id);
            if (el) sessionStorage.setItem('kc_reg_' + id, el.value);
        });
    });

    // On page load: if Keycloak returned an error, restore the saved values
    (function () {
        var hasError = document.querySelector('.alert-error, .alert-warning');
        if (!hasError) {
            // No error — clear any stale saved data
            ['firstName', 'lastName', 'email', 'username'].forEach(function (id) {
                sessionStorage.removeItem('kc_reg_' + id);
            });
            return;
        }
        ['firstName', 'lastName', 'email', 'username'].forEach(function (id) {
            var saved = sessionStorage.getItem('kc_reg_' + id);
            var el = document.getElementById(id);
            if (saved && el && !el.value) {
                el.value = saved;
            }
        });
    })();
</script>
</body>
</html>
