import { createContext, useContext, useEffect, useMemo, useState } from 'react';

const storage_key = 'erp_vinamik_ui_preferences_v1';
const theme_context = createContext(null);

const default_preferences = {
  appearance: 'light',
  accent: 'vinamik_blue',
  density: 'comfortable',
  font_family: 'be_vietnam_pro',
  font_size: 15,
  radius: 14,
};

const accent_options = {
  vinamik_blue: {
    label: 'Xanh Vinamik',
    primary: '#0413b0',
    deep: '#020a6e',
    soft: '#e8eaff',
    contrast: '#ffffff',
  },
  vinamik_mint: {
    label: 'Xanh cobalt Vinamik',
    primary: '#1838c7',
    deep: '#0b1f86',
    soft: '#e9edff',
    contrast: '#ffffff',
  },
  vinamik_purple: {
    label: 'Xanh chàm Vinamik',
    primary: '#5144b8',
    deep: '#30277e',
    soft: '#efedff',
    contrast: '#ffffff',
  },
  vinamik_sky: {
    label: 'Xanh sáng Vinamik',
    primary: '#2f66cf',
    deep: '#1c3c91',
    soft: '#eaf1ff',
    contrast: '#ffffff',
  },
};

const font_options = {
  be_vietnam_pro: {
    label: 'Be Vietnam Pro',
    value: '"Be Vietnam Pro", "Segoe UI", Arial, sans-serif',
  },
  lexend: {
    label: 'Lexend',
    value: '"Lexend", "Be Vietnam Pro", "Segoe UI", Tahoma, sans-serif',
  },
  system: {
    label: 'Segoe UI',
    value: '"Segoe UI", Tahoma, sans-serif',
  },
};

function load_preferences() {
  try {
    const stored_preferences = JSON.parse(window.localStorage.getItem(storage_key) || '{}');
    const merged_preferences = { ...default_preferences, ...stored_preferences };
    if (!Object.prototype.hasOwnProperty.call(accent_options, merged_preferences.accent)) merged_preferences.accent = default_preferences.accent;
    if (![14, 15, 16, 17].includes(Number(merged_preferences.font_size))) merged_preferences.font_size = default_preferences.font_size;
    if (!Object.prototype.hasOwnProperty.call(font_options, merged_preferences.font_family)) merged_preferences.font_family = default_preferences.font_family;
    return merged_preferences;
  } catch {
    return default_preferences;
  }
}

function ThemeProvider({ children }) {
  const [preferences, set_preferences] = useState(load_preferences);

  useEffect(() => {
    try {
      window.localStorage.setItem(storage_key, JSON.stringify(preferences));
    } catch {
      // Theme preferences are optional; the UI still works when storage is unavailable.
    }
    const root = document.documentElement;
    const accent = accent_options[preferences.accent] || accent_options.vinamik_blue;
    const is_dark = preferences.appearance === 'dark';
    const density_unit = preferences.density === 'compact' ? '4px' : '6px';

    root.dataset.theme = is_dark ? 'dark' : 'light';
    root.dataset.density = preferences.density;
    root.style.setProperty('--erp-accent', accent.primary);
    root.style.setProperty('--erp-accent-deep', accent.deep);
    root.style.setProperty('--erp-accent-soft', is_dark ? accent.primary + '26' : accent.soft);
    root.style.setProperty('--erp-accent-contrast', accent.contrast);
    root.style.setProperty('--erp-font-family', font_options[preferences.font_family]?.value || font_options.be_vietnam_pro.value);
    root.style.setProperty('--erp-font-size', String(preferences.font_size) + 'px');
    root.style.setProperty('--erp-density-unit', density_unit);
  }, [preferences]);

  const update_preference = (key, value) => {
    set_preferences((current_preferences) => ({ ...current_preferences, [key]: value }));
  };

  const reset_preferences = () => set_preferences(default_preferences);

  const selected_accent = accent_options[preferences.accent] || accent_options.vinamik_blue;
  const theme_tokens = useMemo(() => {
    const is_dark = preferences.appearance === 'dark';
    return {
      algorithm: is_dark ? 'dark' : 'default',
      token: {
        colorPrimary: selected_accent.primary,
        colorInfo: selected_accent.primary,
        colorSuccess: '#16835b',
        colorWarning: '#c47b18',
        colorError: '#d85454',
        colorBgBase: is_dark ? '#090d2d' : '#f4f6ff',
        colorBgLayout: is_dark ? '#090d2d' : '#f4f6ff',
        colorBgContainer: is_dark ? '#111750' : '#ffffff',
        colorTextBase: is_dark ? '#f0f2ff' : '#17204f',
        colorTextSecondary: is_dark ? '#bfc7f1' : '#53618e',
        colorBorder: is_dark ? '#2b3780' : '#d8def5',
        borderRadius: preferences.radius,
        fontFamily: font_options[preferences.font_family]?.value || font_options.be_vietnam_pro.value,
        fontSize: preferences.font_size,
      },
    };
  }, [preferences, selected_accent]);

  const value = useMemo(() => ({
    preferences,
    accent_options,
    font_options,
    theme_tokens,
    update_preference,
    reset_preferences,
  }), [preferences, theme_tokens]);

  return <theme_context.Provider value={value}>{children}</theme_context.Provider>;
}

function useTheme() {
  const context = useContext(theme_context);
  if (!context) {
    throw new Error('use_theme must be used inside ThemeProvider.');
  }
  return context;
}

const use_theme = useTheme;

export { ThemeProvider, use_theme };

