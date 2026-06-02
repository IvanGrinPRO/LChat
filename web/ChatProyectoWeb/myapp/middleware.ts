import { NextRequest, NextResponse } from "next/server";

export function middleware(request : NextRequest){
    try{
        const {pathname} = request.nextUrl;
            if (!(pathname.startsWith('/pages'))){
                const chatUrl = new URL('/pages/user/chat/123',request.url)
                return NextResponse.redirect(chatUrl)
            }
    }catch (error){
        console.error('Middleware error:', error);
        // Fail-safe: allow request to continue if something goes wrong
        return NextResponse.next();
    }
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};