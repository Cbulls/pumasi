import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: "#4f46e5",
          dark: "#4338ca",
          light: "#eef2ff",
        },
        ink: {
          DEFAULT: "#0f172a",
          soft: "#1e293b",
        },
        accent: {
          DEFAULT: "#0ea5e9",
          soft: "#e0f2fe",
        },
      },
      fontFamily: {
        display: ["var(--font-display)", "Noto Sans KR", "sans-serif"],
        sans: ["var(--font-sans)", "Noto Sans KR", "sans-serif"],
      },
    },
  },
  plugins: [],
};

export default config;
