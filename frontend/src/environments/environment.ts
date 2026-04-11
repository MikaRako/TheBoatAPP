export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api',
  keycloak: {
    issuer: 'http://localhost:8080/realms/boat-realm',
    redirectUri: window.location.origin + '/auth-callback',
    clientId: 'boat-frontend',
    scope: 'openid profile email',
    responseType: 'code',
    showDebugInformation: true,
  }
};
