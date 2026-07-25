import NextAuth from "next-auth";
import Zitadel from "next-auth/providers/zitadel";

export const isAuthConfigured = Boolean(process.env.AUTH_ZITADEL_ISSUER);

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: isAuthConfigured
    ? [
        Zitadel({
          clientId: process.env.AUTH_ZITADEL_ID,
          clientSecret: process.env.AUTH_ZITADEL_SECRET,
          issuer: process.env.AUTH_ZITADEL_ISSUER,
        }),
      ]
    : [],
  session: { strategy: "jwt" },
});
