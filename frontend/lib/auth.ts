import NextAuth from "next-auth";
import Zitadel from "next-auth/providers/zitadel";
import type { JWT } from "next-auth/jwt";

export const isAuthConfigured = Boolean(process.env.AUTH_ZITADEL_ISSUER);

const REFRESH_SKEW_MS = 60_000;

let tokenEndpointCache: string | undefined;

async function tokenEndpoint(): Promise<string> {
  if (tokenEndpointCache) {
    return tokenEndpointCache;
  }
  const issuer = (process.env.AUTH_ZITADEL_ISSUER ?? "").replace(/\/$/, "");
  const response = await fetch(`${issuer}/.well-known/openid-configuration`);
  if (!response.ok) {
    throw new Error(`Discovery failed (${response.status})`);
  }
  const document = await response.json();
  tokenEndpointCache = document.token_endpoint as string;
  return tokenEndpointCache;
}

async function refreshAccessToken(token: JWT): Promise<JWT> {
  if (!token.refreshToken) {
    return { ...token, error: "NoRefreshToken" };
  }
  try {
    const endpoint = await tokenEndpoint();
    const credentials = Buffer.from(
      `${process.env.AUTH_ZITADEL_ID}:${process.env.AUTH_ZITADEL_SECRET}`,
    ).toString("base64");

    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "content-type": "application/x-www-form-urlencoded",
        authorization: `Basic ${credentials}`,
      },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: token.refreshToken,
      }),
    });

    const refreshed = await response.json();
    if (!response.ok) {
      throw new Error(refreshed.error_description ?? refreshed.error ?? `HTTP ${response.status}`);
    }

    return {
      ...token,
      accessToken: refreshed.access_token,
      refreshToken: refreshed.refresh_token ?? token.refreshToken,
      expiresAt: Date.now() + Number(refreshed.expires_in ?? 3600) * 1000,
      error: undefined,
    };
  } catch {
    return { ...token, error: "RefreshAccessTokenError" };
  }
}

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: isAuthConfigured
    ? [
        Zitadel({
          clientId: process.env.AUTH_ZITADEL_ID,
          clientSecret: process.env.AUTH_ZITADEL_SECRET,
          issuer: process.env.AUTH_ZITADEL_ISSUER,
          authorization: {
            params: { scope: "openid profile email offline_access" },
          },
        }),
      ]
    : [],
  session: { strategy: "jwt" },
  callbacks: {
    async jwt({ token, account }) {
      if (account?.access_token) {
        return {
          ...token,
          accessToken: account.access_token,
          refreshToken: account.refresh_token,
          expiresAt: account.expires_at
            ? account.expires_at * 1000
            : Date.now() + 3600 * 1000,
          error: undefined,
        };
      }
      if (token.expiresAt && Date.now() < token.expiresAt - REFRESH_SKEW_MS) {
        return token;
      }
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken;
      session.error = token.error;
      return session;
    },
  },
});
