"use client";

import { useEffect } from "react";
import FingerprintJS from "@fingerprintjs/fingerprintjs";
import { deviceFingerprint } from "@/lib/fingerprint";

declare global {
  interface Window {
    FingerprintJS?: typeof FingerprintJS;
  }
}

export default function FingerprintBoot() {
  useEffect(() => {
    window.FingerprintJS = FingerprintJS;
    void deviceFingerprint();
  }, []);

  return null;
}
