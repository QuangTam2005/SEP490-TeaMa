import Image from 'next/image';
import { cn } from '@/lib/utils';

interface BrandLogoProps {
  title?: string;
  size?: 'sm' | 'md';
  className?: string;
}

export function BrandLogo({
  title = 'Smart Otter',
  size = 'md',
  className,
}: BrandLogoProps) {
  const boxSize = size === 'sm' ? 'h-8 w-8' : 'h-9 w-9';
  const imgDim = size === 'sm' ? 32 : 36;

  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <span
        className={cn(
          'flex shrink-0 items-center justify-center overflow-hidden rounded-lg border border-brand/20 bg-white p-0.5 shadow-sm',
          boxSize
        )}
      >
        <Image
          src="/logo.png"
          alt={title}
          width={imgDim}
          height={imgDim}
          className="h-full w-full object-contain"
          unoptimized
          priority
        />
      </span>
      <span className="text-base font-bold tracking-tight text-foreground">
        {title}
      </span>
    </span>
  );
}
