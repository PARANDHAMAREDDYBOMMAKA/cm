import { CalendarClock, ClipboardList, IndianRupee, Sigma } from "lucide-react";

const checks = [
  {
    icon: Sigma,
    name: "Line items vs total",
    catches: "Itemised line items must sum to the claimed total within 2 percent.",
  },
  {
    icon: CalendarClock,
    name: "Date sanity",
    catches:
      "Discharge before admission, an invoice dated before admission, or any date sitting in the future.",
  },
  {
    icon: IndianRupee,
    name: "Procedure, diagnosis & tariff",
    catches:
      "Whether the procedure fits the diagnosis, and whether the billed amount is sane for the tariff, checked against a 15-condition Indian clinical reference table.",
  },
  {
    icon: ClipboardList,
    name: "Missing critical fields",
    catches: "A required field — patient, provider, diagnosis, amount and the like — is absent.",
  },
];

const conditions = [
  "Cataract",
  "Appendicitis",
  "Hernia",
  "Gallstones",
  "Normal delivery",
  "Caesarean delivery",
  "Dengue",
  "Malaria",
  "Typhoid",
  "Pneumonia",
  "Myocardial infarction",
  "Fracture",
  "Renal calculus",
  "Osteoarthritis",
  "COVID-19",
];

export default function ConsistencySection() {
  return (
    <section id="consistency" className="border-b border-border bg-canvas py-20 sm:py-28">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-ink">Consistency checks</h2>
          <p className="mt-3 text-base leading-relaxed text-secondary">
            Once a document is read, its own numbers and dates are checked against each other —
            and against clinical reality.
          </p>
        </div>

        <dl className="mt-12 grid grid-cols-1 gap-5 sm:grid-cols-2">
          {checks.map(({ icon: Icon, name, catches }) => (
            <div key={name} className="rounded-2xl border border-border bg-surface p-6">
              <div className="flex items-center gap-3">
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand-soft text-brand">
                  <Icon className="size-4.5" />
                </div>
                <dt className="text-base font-semibold text-ink">{name}</dt>
              </div>
              <dd className="mt-3 text-sm leading-relaxed text-secondary">{catches}</dd>
            </div>
          ))}
        </dl>

        <div className="mt-6 rounded-2xl border border-border bg-surface p-6">
          <h3 className="text-sm font-semibold text-ink">Clinical reference table covers 15 conditions</h3>
          <p className="mt-1 text-sm leading-relaxed text-secondary">
            Amount-versus-tariff and procedure-versus-diagnosis checks run against this Indian
            clinical reference set:
          </p>
          <ul className="mt-4 flex flex-wrap gap-2">
            {conditions.map((condition) => (
              <li
                key={condition}
                className="rounded-full border border-border bg-canvas px-3 py-1.5 text-xs font-medium text-secondary"
              >
                {condition}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
