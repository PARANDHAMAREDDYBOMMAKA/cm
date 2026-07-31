export type Capability = {
  key: string;
  name: string;
  configured: boolean;
  detail: string;
};

export type Setting = {
  name: string;
  value: string;
  description: string;
};

export type Settings = {
  capabilities: Capability[];
  thresholds: Setting[];
  auditIntact: boolean;
  auditEvents: number;
  auditMessage: string;
};
