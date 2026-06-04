'use client';

import { useState, useEffect, useRef } from 'react';

// Module-level cache — survives re-renders and route changes within the session
const cachedUrls = new Set<string>();

interface Props {
  src: string;
  alt: string;
  className?: string;
}

export default function AvatarImage({ src, alt, className }: Props) {
  const [displaySrc, setDisplaySrc] = useState(src);
  const pendingSrc = useRef<string | null>(null);

  useEffect(() => {
    if (!src || src === displaySrc) return;

    // Already loaded before — swap instantly, no flicker
    if (cachedUrls.has(src)) {
      setDisplaySrc(src);
      return;
    }

    // Load in background; keep showing current image until new one is ready
    pendingSrc.current = src;
    let cancelled = false;
    const img = new window.Image();
    img.onload = () => {
      if (!cancelled && pendingSrc.current === src) {
        cachedUrls.add(src);
        setDisplaySrc(src);
      }
    };
    img.src = src;
    return () => { cancelled = true; };
  }, [src]);

  // Mark initial src as cached once the img element fires onLoad
  function handleLoad() {
    if (src) cachedUrls.add(src);
  }

  return (
    <img
      src={displaySrc}
      alt={alt}
      className={className}
      onLoad={handleLoad}
    />
  );
}
