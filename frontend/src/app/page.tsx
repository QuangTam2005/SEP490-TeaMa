import type { Metadata } from 'next';
import { LandingHeader } from '@/components/landing/landing-header';
import { LandingSections } from '@/components/landing/landing-sections';

export const metadata: Metadata = {
  title: 'TeaMa — Quản trị đồ án SEP490 trong 15 tuần',
  description:
    'Nền tảng quản trị đồ án tốt nghiệp SEP490: 10 cột mốc OGA, phân rã WBS & Complexity, truy vết SRS đến Test Evidence và đồng bộ GitHub đa repository.',
};

export default function HomePage() {
  return (
    <>
      <LandingHeader />
      <LandingSections />
    </>
  );
}
