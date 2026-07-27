import type { NextConfig } from "next";

const serverHeader = process.env.STACK_SERVER_HEADER;

const nextConfig: NextConfig = {
  async headers() {
    if (!serverHeader) {
      return [];
    }
    return [
      {
        source: "/:path*",
        headers: [{ key: "Server", value: serverHeader }],
      },
    ];
  },
};

export default nextConfig;
