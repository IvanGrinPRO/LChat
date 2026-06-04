import { NextRequest, NextResponse } from "next/server";

export function proxy(request: NextRequest) {
  try {
    const { pathname } = request.nextUrl;

    // if (/\.(.+)$/.test(pathname)) {
    //   return NextResponse.next();
    // }

    // if (pathname === "/") {
    //   return NextResponse.redirect(new URL("/pages/login", request.url));
    // }

    // if (pathname.startsWith("/pages")) {
    //   return NextResponse.next();
    // }

    // return NextResponse.redirect(new URL("/pages/login", request.url));
  } catch {
    return NextResponse.next();
  }
}

export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|.*\\.(?:png|jpg|jpeg|gif|svg|ico|webp)$).*)",
  ],
};
