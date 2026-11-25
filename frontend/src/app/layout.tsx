import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { Be_Vietnam_Pro, JetBrains_Mono } from 'next/font/google';
import { ThemeProvider } from '@/components/providers/theme-provider';
import { QueryProvider } from '@/components/providers/query-provider';
import { AuthHydrator } from '@/components/providers/auth-hydrator';
import { AppToaster } from '@/components/providers/toaster';
import { UiReviewToolbar } from '@/components/shared/ui-review-toolbar';
import { TooltipProvider } from '@/components/ui/tooltip';
import './globals.css';

const beVietnamPro = Be_Vietnam_Pro({
  subsets: ['vietnamese', 'latin'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-be-vietnam-pro',
  display: 'swap',
});

const jetBrainsMono = JetBrains_Mono({
  subsets: ['vietnamese', 'latin'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-jetbrains-mono',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'AI Study Companion',
  description:
    'Nền tảng web AI hỗ trợ học sinh ôn bài cũ, soạn bài mới và theo dõi tiến độ học tập.',
};

interface RootLayoutProps {
  children: ReactNode;
}

export default function RootLayout({ children }: RootLayoutProps) {
  return (
    <html
      lang="vi"
      suppressHydrationWarning
      className={`${beVietnamPro.variable} ${jetBrainsMono.variable}`}
    >
      <body className="min-h-screen bg-canvas font-sans text-slate-900 antialiased dark:bg-background dark:text-foreground">
        <ThemeProvider>
          <QueryProvider>
            <AuthHydrator />
            <TooltipProvider delayDuration={200}>{children}</TooltipProvider>
            <UiReviewToolbar />
            <AppToaster />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
