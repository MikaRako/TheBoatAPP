import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  template: `<p>Authenticating...</p>`
})
export class AuthCallbackComponent implements OnInit {
  constructor(private oauthService: OAuthService, private router: Router) {}

  async ngOnInit(): Promise<void> {
    try {
      await this.oauthService.loadDiscoveryDocument();
      if (typeof (this.oauthService as any).tryLoginCodeFlow === 'function') {
        await (this.oauthService as any).tryLoginCodeFlow();
      } else {
        await (this.oauthService as any).tryLogin();
      }

      if (this.oauthService.hasValidAccessToken()) {
        this.router.navigate(['/boats']);
      } else {
        // fallback: start auth flow again
        this.oauthService.initCodeFlow();
      }
    } catch (err) {
      console.error('Auth callback error:', err);
      this.oauthService.initCodeFlow();
    }
  }
}
