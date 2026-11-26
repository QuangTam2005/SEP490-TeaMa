import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { ThemeProvider } from '@/components/providers/theme-provider';
import { QueryProvider } from '@/components/providers/query-provider';
import { AuthHydrator } from '@/components/providers/auth-hydrator';
import { AppToaster } from '@/components/providers/toaster';
import { UiReviewToolbar } from '@/components/shared/ui-review-toolbar';
import { TooltipProvider } from '@/components/ui/tooltip';
import './fonts/fonts.css';
import './globals.css';

export const metadata: Metadata = {
  title: {
    default: 'Smart Otter',
    template: '%s | Smart Otter',
  },
  description:
    'Smart Otter — Nền tảng web AI hỗ trợ học sinh ôn bài cũ, soạn bài mới và theo dõi tiến độ học tập.',
  icons: {
    icon: '/fav.png',
    shortcut: '/fav.png',
    apple: '/fav.png',
  },
};

interface RootLayoutProps {
  children: ReactNode;
}

export default function RootLayout({ children }: RootLayoutProps) {
  return (
    <html
      lang="vi"
      suppressHydrationWarning
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
