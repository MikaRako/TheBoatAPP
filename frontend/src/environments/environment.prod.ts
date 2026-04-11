export const environment = {
  production: true,
  apiUrl: '/api',
  keycloak: {
    issuer: 'http://localhost:8080/realms/boat-realm',
    redirectUri: window.location.origin + '/auth-callback',
    clientId: 'boat-frontend',
    scope: 'openid profile email',
    responseType: 'code',
    showDebugInformation: false,
  }
};
