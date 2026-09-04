/**
 * =============================================================================
 * CLIENT — home-data.js
 * URL: / (home)
 * File: home-data.js
 * Home page carousel and card data
 * =============================================================================
 */

window.HOME_DATA = {
  pricingPlans: ["starter-plan", "professional-plan", "premium-plan"],
  items: {
    "starter-plan": {
      title: "Starter Plan",
      tag: "Pricing",
      image:
        "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?auto=format&fit=crop&w=1200&q=80",
      summary: "Perfect for beginners and simple one-page website projects.",
      details: [
        "Single-page or landing website",
        "Mobile responsive layout",
        "Contact / WhatsApp button",
        "1 round of content changes",
        "Delivery in 5–7 working days",
      ],
      price: "From ₹2,999",
      link: "/pricing#web-design",
      linkLabel: "View web packages",
    },
    "professional-plan": {
      title: "Professional Plan",
      tag: "Pricing",
      image:
        "https://images.unsplash.com/photo-1561070791-2526d30994b5?auto=format&fit=crop&w=1200&q=80",
      summary: "Best for business website, branding and advanced custom work.",
      details: [
        "Up to 5 pages with pro design",
        "SEO-friendly structure",
        "Business email setup",
        "Google Maps & enquiry forms",
        "2 content revision rounds",
      ],
      price: "From ₹7,999",
      link: "/pricing#web-design",
      linkLabel: "View web packages",
    },
    "premium-plan": {
      title: "Premium Plan",
      tag: "Pricing",
      image:
        "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?auto=format&fit=crop&w=1200&q=80",
      summary: "Full package with website, app support and priority delivery.",
      details: [
        "Website + app consultation",
        "CMS or advanced features",
        "Priority support queue",
        "Store / payment-ready setup",
        "3 revision rounds included",
      ],
      price: "From ₹12,999",
      link: "/pricing",
      linkLabel: "All pricing options",
    },
    "website-dev": {
      title: "Website Development",
      tag: "Service",
      image:
        "https://images.unsplash.com/photo-1547658719-da2b51169166?auto=format&fit=crop&w=1200&q=80",
      summary: "Business, portfolio, landing and e-commerce websites.",
      details: [
        "Custom design matching your brand",
        "Fast loading & mobile-first",
        "Domain, hosting & SSL guidance",
        "E-commerce & booking flows",
        "Post-launch maintenance available",
      ],
      price: "Custom quote",
      link: "/services#website-development",
      linkLabel: "Explore web services",
    },
    "app-dev": {
      title: "App Development",
      tag: "Service",
      image:
        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=1200&q=80",
      summary: "Web app and Android app with clean and modern UI.",
      details: [
        "Android & Flutter cross-platform",
        "Login, dashboard & notifications",
        "Play Store publish assistance",
        "API / Firebase backend",
        "MVP to full product builds",
      ],
      price: "From ₹9,999",
      link: "/pricing#app-development",
      linkLabel: "App pricing",
    },
    "logo-design": {
      title: "Logo Design",
      tag: "Service",
      image:
        "https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=1200&q=80",
      summary: "Professional logo and branding kit creation.",
      details: [
        "Multiple design concepts",
        "PNG, JPG & vector files",
        "Colour palette & fonts",
        "Social media size exports",
        "Brand mini-guide (premium)",
      ],
      price: "From ₹1,499",
      link: "/pricing#logo-design",
      linkLabel: "Logo packages",
    },
    "seo-geo": {
      title: "SEO & GEO",
      tag: "Service",
      image:
        "https://images.unsplash.com/photo-1432888498266-38ffec3eaf0a?auto=format&fit=crop&w=1200&q=80",
      summary: "Google rankings plus visibility in AI search and answer engines.",
      details: [
        "On-page and technical SEO",
        "Generative Engine Optimization (GEO)",
        "Local / geographic search (maps & GBP)",
        "Content and keyword strategy",
        "Monthly reporting and next-step plan",
      ],
      price: "From ₹3,999/mo",
      link: "/services#seo-geo",
      linkLabel: "Explore SEO & GEO",
    },
    "student-projects": {
      title: "Student Projects",
      tag: "Service",
      image:
        "https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80",
      summary: "Final year projects with report and presentation support.",
      details: [
        "Project code & documentation",
        "PPT and viva preparation tips",
        "Web, app & mini software topics",
        "Plagiarism-safe original work",
        "Deadline-focused delivery",
      ],
      price: "Budget-friendly",
      link: "/contact",
      linkLabel: "Get a quote",
    },
  },
  showcase: [
    {
      title: "Business Website",
      brand: "Shrisha Technology Client",
      image:
        "https://images.unsplash.com/photo-1547658719-da2b51169166?auto=format&fit=crop&w=1200&q=80",
      description:
        "Modern business website with mobile layout, enquiry forms and fast loading — built for local brands going online.",
      homeId: "website-dev",
    },
    {
      title: "E-Commerce Store",
      brand: "Online Shop",
      image:
        "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=1200&q=80",
      description:
        "Product catalog, cart-ready structure and payment gateway preparation for small retailers.",
      homeId: "premium-plan",
    },
    {
      title: "Android App",
      brand: "Startup MVP",
      image:
        "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?auto=format&fit=crop&w=1200&q=80",
      description:
        "Clean Material UI app with login flow and API integration — ideal for startups testing an idea.",
      homeId: "app-dev",
    },
    {
      title: "Student Final Year Project",
      brand: "Engineering College",
      image:
        "https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80",
      description:
        "Full stack student project with report, PPT support and viva guidance — delivered on deadline.",
      homeId: "student-projects",
    },
    {
      title: "SEO & GEO",
      brand: "Search Visibility",
      image:
        "https://images.unsplash.com/photo-1432888498266-38ffec3eaf0a?auto=format&fit=crop&w=1200&q=80",
      description:
        "Rank on Google and get cited in AI answers — SEO plus GEO for maps, ChatGPT, Perplexity, and AI Overviews.",
      homeId: "seo-geo",
    },
    {
      title: "Brand & Logo Kit",
      brand: "New Business",
      image:
        "https://images.unsplash.com/photo-1561070791-2526d30994b5?auto=format&fit=crop&w=1200&q=80",
      description:
        "Logo concepts, colour palette and social sizes so your brand looks professional everywhere.",
      homeId: "logo-design",
    },
  ],
};
