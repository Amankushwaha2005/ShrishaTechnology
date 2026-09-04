/**
 * =============================================================================
 * CLIENT — pricing-data.js
 * URL: /pricing
 * File: pricing-data.js
 * Pricing plans and package data
 * =============================================================================
 */

/* Pricing categories + packages — loaded by pricing-page.js */
window.PRICING_DATA = {
  categories: [
    {
      id: "web-design",
      title: "Web Design",
      subtitle: "Website design packages",
      image:
        "https://images.unsplash.com/photo-1547658719-da2b51169166?auto=format&fit=crop&w=800&q=80",
      heroTitle: "Website design cost in India",
      heroText:
        "Professional websites with domain, hosting, SSL and support. Packages start from ₹2,999 — choose options below and enquire.",
      packages: [
        {
          ribbon: "Economy",
          ribbonClass: "ribbon-economy",
          title: "For Startups",
          description:
            "Ideal for startups needing a professional website and business email with minimal content.",
          price: 2999,
          priceLabel: "₹2,999",
          features: [
            "One page website",
            "Business email setup",
            "Mobile responsive",
            "Enquiry form",
            "WhatsApp chat button",
            "Click to call option",
            "1 content revision",
          ],
          options: [
            { label: "Domain", choices: [".com", ".in / .co.in"] },
            { label: "Hosting", choices: ["500 MB", "1 GB"] },
            { label: "SSL", choices: ["Basic", "Professional"] },
          ],
        },
        {
          ribbon: "Professional",
          ribbonClass: "ribbon-pro",
          title: "For Growing Businesses",
          description:
            "Perfect for established companies with detailed product or service content ready to showcase.",
          price: 7999,
          priceLabel: "₹7,999",
          features: [
            "Professional web design",
            "Up to 5 static pages",
            "Business email (2 IDs)",
            "SEO-friendly meta tags",
            "Mobile-first layout",
            "Contact & enquiry forms",
            "Google Maps embed",
            "2 content revisions",
          ],
          options: [
            { label: "Domain", choices: [".com", ".in / .co.in"] },
            { label: "Hosting", choices: ["1 GB", "2 GB"] },
            { label: "SSL", choices: ["Basic", "Professional"] },
          ],
        },
        {
          ribbon: "CMS",
          ribbonClass: "ribbon-cms",
          title: "For Self-Managed Websites",
          description:
            "Best for businesses that want to update website content themselves without a developer.",
          price: 12999,
          priceLabel: "₹12,999",
          features: [
            "Custom web design",
            "6 static pages + 2 CMS pages",
            "CMS admin login",
            "Blog / news section",
            "Business emails (3 IDs)",
            "On-page SEO setup",
            "Training for content updates",
            "3 content revisions",
          ],
          options: [
            { label: "Domain", choices: [".com", ".in / .co.in"] },
            { label: "Hosting", choices: ["1 GB", "2 GB"] },
            { label: "SSL", choices: ["Basic", "Professional"] },
          ],
        },
      ],
      extras: {
        delivery: "Your website will be ready within 10–15 working days (scope dependent).",
        source: "Complete source files provided after go-live (as per package).",
        maintenance:
          "One-year free maintenance for bug fixes and small tweaks. New pages or major changes are quoted separately.",
        steps: [
          {
            title: "Professional domain-based email",
            text: "Build trust with clients using your own domain email addresses.",
          },
          {
            title: "Secure domain ownership",
            text: "Domain registered in your name with clear renewal reminders.",
          },
          {
            title: "Google visibility basics",
            text: "Search-friendly structure so customers can find you online.",
          },
          {
            title: "Brand-ready design",
            text: "Clean layout aligned with your logo and business identity.",
          },
        ],
      },
    },
    {
      id: "app-development",
      title: "App Development",
      subtitle: "Android, iOS & cross-platform apps",
      image:
        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80",
      heroTitle: "App development cost in India",
      heroText:
        "Native and cross-platform mobile apps with modern UI. Packages from ₹9,999 — MVP to full product builds.",
      packages: [
        {
          ribbon: "MVP",
          ribbonClass: "ribbon-economy",
          title: "Starter App",
          description: "Simple Android or hybrid app for startups and student projects.",
          price: 9999,
          priceLabel: "₹9,999",
          features: [
            "Up to 5 screens",
            "Android APK build",
            "Clean Material UI",
            "Login / signup flow",
            "API integration (basic)",
            "1 revision round",
          ],
          options: [
            { label: "Platform", choices: ["Android", "Flutter (both)"] },
            { label: "Backend", choices: ["Firebase", "Custom API"] },
          ],
        },
        {
          ribbon: "Business",
          ribbonClass: "ribbon-pro",
          title: "Business App",
          description: "Feature-rich app for SMEs — bookings, catalog, payments-ready flows.",
          price: 24999,
          priceLabel: "₹24,999",
          features: [
            "Up to 12 screens",
            "Admin panel (web)",
            "Push notifications",
            "Payment gateway ready",
            "Play Store publish help",
            "2 revision rounds",
          ],
          options: [
            { label: "Platform", choices: ["Android", "Android + iOS"] },
            { label: "Hosting", choices: ["Shared API", "Dedicated VPS"] },
          ],
        },
        {
          ribbon: "Premium",
          ribbonClass: "ribbon-cms",
          title: "Full Product App",
          description: "End-to-end product with analytics, roles and scalable architecture.",
          price: 49999,
          priceLabel: "₹49,999+",
          features: [
            "Custom UI/UX design",
            "Android + iOS (Flutter)",
            "Role-based access",
            "Analytics dashboard",
            "Store listing assets",
            "3 months support",
          ],
          options: [
            { label: "Scope", choices: ["Standard", "Enterprise (quote)"] },
            { label: "Maintenance", choices: ["3 months", "6 months"] },
          ],
        },
      ],
      extras: {
        delivery: "MVP apps: 3–4 weeks. Business apps: 6–10 weeks depending on scope.",
        source: "Full source code and build files handed over after final payment.",
        maintenance: "Bug fixes included per package. New features quoted separately.",
        steps: [
          {
            title: "UI/UX first",
            text: "Wireframes and app flow approved before development starts.",
          },
          {
            title: "Store-ready build",
            text: "Signed APK/AAB and guidance for Play Store / App Store submission.",
          },
          {
            title: "Backend & APIs",
            text: "Secure login, data sync and admin tools as per your package.",
          },
          {
            title: "Post-launch support",
            text: "Help with crashes, OS updates and small fixes during support period.",
          },
        ],
      },
    },
    {
      id: "website-maintenance",
      title: "Website Maintenance",
      subtitle: "Keep your site fast and secure",
      image:
        "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: "Basic",
          ribbonClass: "ribbon-economy",
          title: "Essential Care",
          description: "Monthly checks, updates and backup for small business sites.",
          price: 999,
          priceLabel: "₹999/mo",
          features: ["Plugin / dependency updates", "Uptime monitoring", "Monthly backup", "1 small content edit"],
          options: [{ label: "Support", choices: ["Email", "WhatsApp"] }],
        },
        {
          ribbon: "Pro",
          ribbonClass: "ribbon-pro",
          title: "Business Care",
          description: "Priority support for growing sites with regular content changes.",
          price: 2499,
          priceLabel: "₹2,499/mo",
          features: [
            "Everything in Essential",
            "Security scan",
            "Speed optimisation",
            "Up to 5 content edits/month",
            "Quarterly performance report",
          ],
          options: [{ label: "Response", choices: ["24–48 hrs", "Same day (limited)"] }],
        },
      ],
    },
    {
      id: "digital-marketing",
      title: "Digital Marketing",
      subtitle: "Reach more customers online",
      image:
        "https://images.unsplash.com/photo-1551434678-e076c223a692?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: "Starter",
          ribbonClass: "ribbon-economy",
          title: "Launch Pack",
          description: "Ads setup and landing page alignment for new campaigns.",
          price: 4999,
          priceLabel: "₹4,999",
          features: ["Campaign structure", "2 ad creatives", "Landing page review", "Basic analytics"],
          options: [{ label: "Platform", choices: ["Google Ads", "Meta Ads"] }],
        },
        {
          ribbon: "Growth",
          ribbonClass: "ribbon-pro",
          title: "Growth Pack",
          description: "Ongoing optimisation and reporting for steady leads.",
          price: 9999,
          priceLabel: "₹9,999/mo",
          features: ["Multi-channel ads", "A/B testing", "Weekly reports", "Conversion tracking"],
          options: [{ label: "Budget", choices: ["Managed", "Advisory only"] }],
        },
      ],
    },
    {
      id: "social-media",
      title: "Social Media Packages",
      subtitle: "Posts, reels and brand consistency",
      image:
        "https://images.unsplash.com/photo-1611162616305-c69b3fa7fbe0?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: "Lite",
          ribbonClass: "ribbon-economy",
          title: "Social Lite",
          description: "8 posts per month for one platform.",
          price: 3499,
          priceLabel: "₹3,499/mo",
          features: ["8 feed posts", "Caption + hashtags", "1 platform", "Monthly calendar"],
          options: [{ label: "Platform", choices: ["Instagram", "Facebook", "LinkedIn"] }],
        },
        {
          ribbon: "Plus",
          ribbonClass: "ribbon-cms",
          title: "Social Plus",
          description: "Posts + reels for two platforms with engagement tips.",
          price: 6999,
          priceLabel: "₹6,999/mo",
          features: ["12 posts + 4 reels", "2 platforms", "Story templates", "Monthly analytics"],
          options: [{ label: "Reels", choices: ["4/month", "8/month"] }],
        },
      ],
    },
    {
      id: "seo",
      title: "SEO & GEO Packages",
      subtitle: "Rank on Google and show up in AI search",
      image:
        "https://images.unsplash.com/photo-1432888498266-38ffec3eaf0a?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: "Local",
          ribbonClass: "ribbon-economy",
          title: "Local SEO",
          description: "Google Business Profile and local keyword focus.",
          price: 3999,
          priceLabel: "₹3,999/mo",
          features: ["GBP optimisation", "Local keywords", "Citation basics", "Monthly report"],
          options: [{ label: "City", choices: ["1 city", "Multi-city"] }],
        },
        {
          ribbon: "National",
          ribbonClass: "ribbon-pro",
          title: "National SEO",
          description: "On-page + content plan for broader search visibility.",
          price: 8999,
          priceLabel: "₹8,999/mo",
          features: ["Technical audit", "Content plan", "Link building guidance", "Bi-weekly reports"],
          options: [{ label: "Pages", choices: ["Up to 10", "Up to 25"] }],
        },
        {
          ribbon: "GEO",
          ribbonClass: "ribbon-cms",
          title: "SEO + GEO",
          description: "Classic SEO plus Generative Engine Optimization for AI answers.",
          price: 12999,
          priceLabel: "₹12,999/mo",
          features: [
            "SEO + GEO content structure",
            "Schema & FAQ for AI Overviews",
            "Entity / brand fact pages",
            "Monthly SEO & GEO report",
          ],
          options: [{ label: "Scope", choices: ["Core pages", "Site-wide"] }],
        },
      ],
    },
    {
      id: "logo-design",
      title: "Logo Design",
      subtitle: "Professional brand identity",
      image:
        "https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: "Basic",
          ribbonClass: "ribbon-economy",
          title: "Logo Starter",
          description: "2 concepts, 1 final logo with PNG and JPG.",
          price: 1499,
          priceLabel: "₹1,499",
          features: ["2 design concepts", "2 revisions", "PNG + JPG", "Colour palette"],
          options: [{ label: "Files", choices: ["Raster only", "+ SVG"] }],
        },
        {
          ribbon: "Brand",
          ribbonClass: "ribbon-pro",
          title: "Brand Kit",
          description: "Logo + social sizes + simple brand guide.",
          price: 3999,
          priceLabel: "₹3,999",
          features: ["3 concepts", "Logo variations", "Social media kit", "Mini brand guide PDF"],
          options: [{ label: "Extras", choices: ["Business card", "Letterhead"] }],
        },
      ],
    },
    {
      id: "domain",
      title: "Domain Registration",
      subtitle: "Your name on the web",
      image:
        "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: ".COM",
          ribbonClass: "ribbon-economy",
          title: ".com Domain",
          description: "Register or transfer a .com domain for 1 year.",
          price: 899,
          priceLabel: "₹899/yr",
          features: ["WHOIS privacy (where available)", "DNS management", "Renewal reminders", "Email forwarding setup"],
          options: [{ label: "Term", choices: ["1 year", "2 years"] }],
        },
        {
          ribbon: ".IN",
          ribbonClass: "ribbon-pro",
          title: ".in / .co.in",
          description: "Indian domain for local businesses.",
          price: 599,
          priceLabel: "₹599/yr",
          features: ["DNS panel", "Transfer support", "Lock & security", "Setup assistance"],
          options: [{ label: "Extension", choices: [".in", ".co.in"] }],
        },
      ],
    },
    {
      id: "hosting",
      title: "Hosting Services",
      subtitle: "Fast and reliable hosting",
      image:
        "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: "500MB",
          ribbonClass: "ribbon-economy",
          title: "Starter Hosting",
          description: "Shared hosting for brochure websites.",
          price: 1499,
          priceLabel: "₹1,499/yr",
          features: ["500 MB SSD", "Free SSL (basic)", "1 website", "Email accounts (2)"],
          options: [{ label: "Backup", choices: ["Weekly", "Daily"] }],
        },
        {
          ribbon: "2GB",
          ribbonClass: "ribbon-pro",
          title: "Business Hosting",
          description: "More space for CMS and growing traffic.",
          price: 2999,
          priceLabel: "₹2,999/yr",
          features: ["2 GB SSD", "SSL included", "3 websites", "Priority support", "Daily backup"],
          options: [{ label: "DB", choices: ["1 MySQL", "3 MySQL"] }],
        },
      ],
    },
    {
      id: "ssl",
      title: "SSL Certificate",
      subtitle: "Secure HTTPS for trust",
      image:
        "https://images.unsplash.com/photo-1563986768609-322da13575f3?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: "Basic",
          ribbonClass: "ribbon-economy",
          title: "Basic SSL",
          description: "Standard encryption for blogs and small sites.",
          price: 0,
          priceLabel: "Free*",
          features: ["Domain validation", "HTTPS padlock", "Auto-renew with hosting", "Setup included"],
          options: [{ label: "Sites", choices: ["1 domain", "Wildcard (+cost)"] }],
        },
        {
          ribbon: "Pro",
          ribbonClass: "ribbon-pro",
          title: "Professional SSL",
          description: "Organisation validation for e-commerce trust.",
          price: 4999,
          priceLabel: "₹4,999/yr",
          features: ["OV certificate", "Site seal", "Priority install", "Mixed content check"],
          options: [{ label: "Type", choices: ["OV", "EV (quote)"] }],
        },
      ],
    },
    {
      id: "business-email",
      title: "Business Email",
      subtitle: "you@yourcompany.com",
      image:
        "https://images.unsplash.com/photo-1598257006458-087169a1f2d4?auto=format&fit=crop&w=800&q=80",
      packages: [
        {
          ribbon: "2 IDs",
          ribbonClass: "ribbon-economy",
          title: "Email Starter",
          description: "Two professional mailboxes on your domain.",
          price: 999,
          priceLabel: "₹999/yr",
          features: ["2 email IDs", "Webmail access", "Mobile sync", "Spam filter"],
          options: [{ label: "Storage", choices: ["2 GB each", "5 GB each"] }],
        },
        {
          ribbon: "5 IDs",
          ribbonClass: "ribbon-pro",
          title: "Email Team",
          description: "Five mailboxes for small teams.",
          price: 2499,
          priceLabel: "₹2,499/yr",
          features: ["5 email IDs", "Shared calendars", "Aliases", "Migration help"],
          options: [{ label: "Provider", choices: ["Google Workspace setup", "Zoho / cPanel"] }],
        },
      ],
    },
  ],
};
