# Vinamik interface direction

<!-- impeccable:design-schema 1 -->

## Visual world

Vinamik uses the visual language of a factory control board: a deep ink navigation rail, a quiet paper workspace, thin measurement rules, and module rows that behave like lanes on an operations board. The interface is calm enough for long data sessions but has a clear point of view through its rail, indexing and module-specific accent.

The dashboard's first viewport is a control room: one direct statement, one primary action, and the module matrix immediately underneath. The matrix is the signature component. A module is a row with one identity column and five function lanes, so the hierarchy from module to its five functions is visible without nesting cards.

## Palette and material

- Light workspace: cool paper #f3f5f8, white work surface, ink text #17243a, blue-gray secondary text, and #dce3ed rules.
- Dark workspace: tinted navy #111a28 and #19263a; never use pure black or neutral gray as the main surface.
- Navigation: ink navy #17263c in light mode and #0d1726 in dark mode.
- Cobalt is the default action accent. Jade, violet and coral are user-selectable accents and are reserved for actions, focus, status and module identity.
- Depth comes from thin rules and soft offset shadows on a small number of surfaces. Avoid glass decoration, gradient fills and nested cards.

## Typography

Be Vietnam Pro is the preferred Vietnamese face, with Lexend and Segoe UI as user-selectable fallbacks. Body text starts at 15px, table text at 14px, labels at 12px, and display headings scale up to 54px. Numerals in data tables use tabular figures where supported. Headings use weight and size for emphasis instead of gradient text or all-caps paragraphs.

## Layout and components

- Desktop content is capped at 1430px inside a fluid application shell.
- The sidebar is a stable navigation rail; the active route uses an inset accent rule and a solid tinted background.
- The dashboard module matrix is a two-column desktop grid and a stacked lane layout on small screens.
- Buttons and inputs have at least 38px touch height, clear focus rings and a short press response.
- Tables remain the primary data surface; horizontal overflow is preserved rather than shrinking data into unreadable cards.
- The customizer controls light/dark appearance, accent, font, font size, density and radius. These preferences are display-only and never contain business data.

## Motion and states

Use one short soft-rise transition for route content and a short press/hover response for controls. Respect prefers-reduced-motion. Loading, empty, error, disabled and permission-denied states must remain visible and actionable. Never hide important content behind a long entrance animation.

## Responsive behavior

At widths below 960px, the module matrix keeps its identity row and changes the function lanes into a compact grid. Below 720px the navigation becomes an overlay, the hero signal moves under the copy, and each module function receives a large touch target. Every route remains usable with keyboard navigation and without hover.

## Content rules

Visible interface copy is Vietnamese. API success/error messages remain English as required by the project contract. Do not invent business metrics; dashboard counts only reflect permissions and known records returned by the backend.
