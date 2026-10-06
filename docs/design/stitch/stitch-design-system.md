---
name: Forest Precision
colors:
  surface: '#f5fbf6'
  surface-dim: '#d5dbd7'
  surface-bright: '#f5fbf6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff5f0'
  surface-container: '#e9efea'
  surface-container-high: '#e4eae5'
  surface-container-highest: '#dee4df'
  on-surface: '#171d1a'
  on-surface-variant: '#414943'
  inverse-surface: '#2c322f'
  inverse-on-surface: '#ecf2ed'
  outline: '#717973'
  outline-variant: '#c1c8c1'
  surface-tint: '#3d6751'
  primary: '#002718'
  on-primary: '#ffffff'
  primary-container: '#133e2b'
  on-primary-container: '#7da990'
  inverse-primary: '#a3d1b6'
  secondary: '#2b6a47'
  on-secondary: '#ffffff'
  secondary-container: '#acefc2'
  on-secondary-container: '#2f6e4b'
  tertiary: '#00271f'
  on-tertiary: '#ffffff'
  tertiary-container: '#003f33'
  on-tertiary-container: '#71ab9a'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#bfedd2'
  primary-fixed-dim: '#a3d1b6'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#254f3a'
  secondary-fixed: '#aff1c5'
  secondary-fixed-dim: '#94d5aa'
  on-secondary-fixed: '#002110'
  on-secondary-fixed-variant: '#0c5131'
  tertiary-fixed: '#b2efdb'
  tertiary-fixed-dim: '#97d2c0'
  on-tertiary-fixed: '#002019'
  on-tertiary-fixed-variant: '#105042'
  background: '#f5fbf6'
  on-background: '#171d1a'
  surface-variant: '#dee4df'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '600'
    lineHeight: 64px
  display-md:
    fontFamily: Inter
    fontSize: 45px
    fontWeight: '600'
    lineHeight: 52px
  display-md-mobile:
    fontFamily: Inter
    fontSize: 34px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-sm:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  gutter-tablet: 1.25rem
  margin: 1.25rem
  margin-mobile: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes an architectural, high-fidelity experience tailored for intelligent personal finance management, machine-assisted receipt digitisation, and computational expense intelligence on Android.

The visual direction follows a calibrated **Modern Corporate Minimalist** aesthetic informed by Google's Material 3 Expressive guidelines. It intentionally avoids playful gamification, cartoonish avatars, or juvenile roundedness in favor of technical rigor, authoritative calm, and surgical clarity. The product evokes the trust of institutional wealth management coupled with the operational speed of modern developer tools.

Visual characteristics focus on:
- High-information density balanced through disciplined vertical rhythm and whitespace.
- Restrained surface tonal layering over drop shadows.
- Precise geometric framing for automated receipt scanning, computer vision targeting, and extraction feedback.
- Restrained, desaturated semantic markers for transaction categorical breakdowns that preserve visual hierarchy without clutter.

## Colors

The palette operates under Material 3 dynamic tonal principles, centered on deep botanical greens and precision mint accents.

### Core Roles
- **Primary (`#133E2B`)**: Deep Pine Emerald. Anchors high-priority actions, primary action containers, key navigation indicators, and app structural headers.
- **Secondary (`#A3E5B9`)**: Frosted Mint. Functions as the active accent, providing high-contrast confirmation on dark containers, active FAB state fills, scanning reticle indicators, and transaction verification markers.
- **Tertiary (`#286354`)**: Subdued Teal-Pine. Used for analytical charts, secondary progress rings, and system-level metadata.
- **Neutral Surface Palette**:
  - `surface`: `#FBFDF9`
  - `surface-dim`: `#DADFD9`
  - `surface-container-lowest`: `#FFFFFF`
  - `surface-container-low`: `#F4F7F3`
  - `surface-container`: `#EDF1ED`
  - `surface-container-high`: `#E7EBE7`
  - `surface-container-highest`: `#E1E6E1`
  - `on-surface`: `#191C1A`
  - `on-surface-variant`: `#414942`
  - `outline`: `#717971`
  - `outline-variant`: `#C1C9C0`

### Semantic Financial Categories
Categories employ desaturated, muted tones to prevent cognitive overload in lists and data tables:
- **Groceries & Food**: Muted Amber Sand (`#C87D32` on light, `#382006` container)
- **Transit & Transport**: Steel Blue Slate (`#4A6B82` on light, `#10212E` container)
- **Leisure & Culture**: Subdued Dusty Plum (`#7A587A` on light, `#29152B` container)
- **Housing & Utilities**: Muted Olive Moss (`#5D6E3F` on light, `#1B250E` container)
- **Health & Wellness**: Technical Seafoam Teal (`#2E776E` on light, `#082522` container)
- **Uncategorized / Other**: Neutral Slate Stone (`#5F6561` on light, `#1E211F` container)

## Typography

Typography prioritizes tabular numerical clarity, legibility at scale, and dense transactional parsing. **Inter** is deployed across all roles, taking advantage of its balanced vertical metrics, clear distinct apertures, and native tabular figure support (`tnum`, `zero`).

### Metric Usage & Special Rules
- **Financial Balance Counters**: Use `display-md` (desktop/tablet) or `display-md-mobile` (handheld) with tabular figures enabled (`font-feature-settings: "tnum" 1, "zero" 1`) to eliminate character jitter during live balance updates.
- **Currency Symbols**: Prefixing amounts with symbols (e.g., `$`, `€`) should use `title-lg` aligned to the cap-height of the adjacent display numeral to maintain optical weight balance.
- **OCR Confidence Ratings & Metadata**: Monitored via `label-sm` in all-caps with `letterSpacing: 0.5px` and `font-weight: 600`.
- **Receipt Itemization**: Use `body-md` for product names and `title-sm` with tabular lining figures for associated prices.

## Layout & Spacing

The layout is built on an adaptive 8-point base grid system (with a secondary 4-point micro-mesh for dense data layouts).

### Breakpoints & Canvas Models
- **Compact (Phones, < 600dp)**: 4-column layout with `margin-mobile` (16dp) margins and `gutter-mobile` (12dp) gutters. Bottom navigation bar and bottom app bar structures anchor all primary interactions. Single-column stacked lists for expense logs.
- **Medium (Foldables & Tablets, 600dp – 839dp)**: 8-column layout with `margin-tablet` (24dp) margins. Layout switches to an asymmetrical split screen: persistent camera/OCR stream on the left (4 columns), live extraction data ledger on the right (4 columns).
- **Expanded (Large Tablets & Desktop Dex, ≥ 840dp)**: 12-column fixed/fluid Hybrid canvas with a max content container width of 1240dp, `margin-desktop` (32dp), using a persistent navigation rail (80dp).

### Safe Areas & Layout Anchors
All scrollable feeds enforce an explicit bottom inset buffer of `space-xl` plus the standard navigation bar height (`80dp`) plus the Floating Action Button clearance (`56dp + 16dp`) to guarantee zero occlusion of the bottom-most list items.

## Elevation & Depth

This design system adheres to Material 3 **Tonal Elevation** instead of traditional diffused drop shadows. Elevation is conveyed by progressively shifting surface colors upward through the container tonal spectrum, paired with hairline low-contrast outlines.

### Elevation Hierarchy
- **Level 0 (Base Canvas)**: `surface` (`#FBFDF9`). Flat baseline.
- **Level 1 (Card & Content Blocks)**: `surface-container-low` (`#F4F7F3`) with an optional `1px` border of `outline-variant` with `0.4` opacity. Zero box shadow. Used for default transaction rows, analytical summaries, and unselected filters.
- **Level 2 (Active Elements & Elevated Cards)**: `surface-container` (`#EDF1ED`). Used for interactive metric tiles, highlighted OCR fields, and hover/active states.
- **Level 3 (Floating Sheets, Drawers, & FABs)**: `surface-container-high` (`#E7EBE7`) or primary tonal containers. Casts a calibrated directional ambient shadow: `offset: (0, 4px)`, `blur: 12px`, `color: rgba(19, 62, 43, 0.08)`.
- **Level 4 (Dialogs & Scanning Overlays)**: `surface-container-highest` (`#E1E6E1`) with ambient soft focus blur: `offset: (0, 8px)`, `blur: 24px`, `color: rgba(0, 0, 0, 0.12)`.

## Shapes

The geometric vocabulary balances ergonomics with architectural discipline. Form factors use strict corner values to convey technical precision:

- **Extra-Small (4dp)**: Applied to micro-indicators, confidence badges, table dividers, and progress bar ends.
- **Small (8dp)**: Input fields, segmented control items, dropdown menus, and camera reticle interior indicators.
- **Medium (12dp)**: Standard Cards (`ElevatedCard`, `OutlinedCard`, `FilledCard`), filter chips, and dialog corners.
- **Large (16dp)**: Bottom sheets, modal surfaces, and scan-result review sheets.
- **Full / Pill (9999dp)**: Material 3 active navigation pills, Extended Floating Action Button (FAB), status tags, and primary action buttons.

## Components

### Buttons
- **Primary Filled Button**: Height 44dp, corner radius 9999dp (pill). Background: `primary` (`#133E2B`), text/icon: `white` (`#FFFFFF`). `label-lg` font weight 600.
- **Tonal Button**: Background `surface-container-highest`, text `primary`. Used for secondary actions like "Add Note" or "Split Expense".
- **Outlined Button**: Height 44dp, 1px border `outline-variant`, text `on-surface`.

### Floating Action Button (FAB)
- **Extended FAB**: Standard height 56dp. Used exclusively for "Scan Receipt" on primary screens. Background: `primary` (`#133E2B`), icon and text: `secondary` (`#A3E5B9`). Expands to 56dp circle with an icon when scrolling feeds.
- **Standard FAB**: 56x56dp rounded square with 16dp corner radius. Placed with 16dp margin from screen edge and bottom bar.

### Bottom Navigation Bar
- Height 80dp. Background: `surface-container-low` (`#F4F7F3`).
- Active item marked with a pill indicator (64x32dp, corner radius 9999dp) filled with `secondary` (`#A3E5B9`) containing an icon in `primary` (`#133E2B`).
- Inactive items display icons in `on-surface-variant` (`#414942`) with `label-md` text below.

### Cards (Tiers)
- **FilledCard**: Background `surface-container`, no border. Used for standard summary lists.
- **OutlinedCard**: Background `surface`, 1px border `outline-variant`. Used for individual extracted line-items from OCR.
- **ElevatedCard**: Background `surface-container-lowest`, Level 1 ambient shadow. Used for high-priority monthly summary totals.

### Filter & Input Chips
- Height 32dp, corner radius 8dp. 
- Inactive state: 1px `outline-variant` border, background transparent, `label-md` text in `on-surface-variant`.
- Active state: Background `secondary` (`#A3E5B9`), text `primary` (`#133E2B`), leading checkmark or category icon.

### Form Inputs & Text Fields
- Material 3 Filled style with underlying indicator line. Background: `surface-container-highest`.
- Focused state: 2px bottom border in `primary`, floating label in `primary`.
- Currency Input: Right-aligned numerical entry using `headline-md` tabular figures with currency indicator locked to the left.

### Camera & OCR Scanning Viewfinder
- Fullscreen camera viewfinder with a 65% darkened scrim around the capture zone.
- Reticle: Precision-cut rectangular frame with 24dp corner brackets (3px thickness) in `secondary` (`#A3E5B9`).
- Live AI Extraction Guides: Transient bounding boxes over recognized text lines rendered in `rgba(163, 229, 185, 0.25)` fill with a 1px solid `secondary` outline and confidence tags rendered in `label-sm`.