import Script from "next/script";

export default function SupportWidget() {
  const propertyId = process.env.NEXT_PUBLIC_TAWK_PROPERTY_ID;
  const widgetId = process.env.NEXT_PUBLIC_TAWK_WIDGET_ID;

  if (!propertyId || !widgetId) {
    return null;
  }

  return (
    <Script
      id="tawk-to"
      src={`https://embed.tawk.to/${propertyId}/${widgetId}`}
      strategy="afterInteractive"
    />
  );
}
