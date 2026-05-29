import type { Metadata } from "next";
import "@/app/globals.css";
import Sidebar from "@/app/components/SideBar";


export const metadata: Metadata = {
  title: "Chat App",
  description: "Neumorphic Chat Design",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="bg-[#1a1a1e] antialiased">
        <div className="flex">
          <Sidebar />

          <main className="flex-grow ml-[280px] min-h-screen">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}