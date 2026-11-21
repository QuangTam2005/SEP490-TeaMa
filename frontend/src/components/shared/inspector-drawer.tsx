'use client';

import type { ReactNode } from 'react';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';

export interface InspectorDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  codeBadge?: string;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
}

/**
 * Standardized right slide-over inspector drawer used for Traceability Pill Links,
 * Use Case details, WBS Task details, and Test Evidence galleries.
 */
export function InspectorDrawer({
  open,
  onOpenChange,
  codeBadge,
  title,
  description,
  children,
  footer,
}: InspectorDrawerProps) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className="flex w-full flex-col gap-0 p-0 sm:max-w-lg"
      >
        <SheetHeader className="border-b px-6 py-4 text-left">
          {codeBadge ? (
            <span className="mb-1 inline-flex w-fit items-center rounded border bg-muted px-2 py-0.5 font-mono text-xs font-medium text-foreground">
              {codeBadge}
            </span>
          ) : null}
          <SheetTitle className="text-base font-semibold">{title}</SheetTitle>
          {description ? (
            <SheetDescription className="text-xs">
              {description}
            </SheetDescription>
          ) : null}
        </SheetHeader>

        <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>

        {footer ? (
          <SheetFooter className="border-t bg-muted/30 px-6 py-3">
            {footer}
          </SheetFooter>
        ) : null}
      </SheetContent>
    </Sheet>
  );
}
