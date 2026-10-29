# la LODKA — Android menu

Android app project for the restaurant menu at https://lodka.moba.market/.

## Included
- black-and-gold visual style and launcher icon based on the La LODKA wordmark;
- menu loaded from the restaurant site, dish photos, names, prices and portion weights when available;
- category chips and search;
- local cache so previously loaded menu remains visible offline;
- manual refresh;
- dish detail dialog (name, photo, price and weight); if the source site does not expose ingredients/description in the parsed menu, the app says so instead of inventing ingredients and links to the official menu;
- Reviews tab with excerpts visible on the restaurant site and a link to leave a review;
- Contacts tab with address, phone, opening hours and site links;
- footer credit: “Сделано Ошурковым А.П.”;
- GitHub Actions workflow to build a debug APK.

## Build
Push the project files to the repository's `main` branch or run **Actions → Build Android APK → Run workflow**. Download the `la-lodka-menu-debug-apk` artifact and install `app-debug.apk` on Android.

## Important
The website may change its markup, which can affect automatic menu parsing. The ingredients and detailed descriptions are not fabricated: the current site parser extracts menu cards, names, images, prices and weights where available; for extra details, the dish dialog links to the official site.
