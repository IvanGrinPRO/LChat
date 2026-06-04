import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Варіант, якщо TypeScript вимагає його в корені:
  allowedDevOrigins: ['26.139.19.234', '26.139.19.234:3000'],
};

export default nextConfig;