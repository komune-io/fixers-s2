import { ThemeContextProvider } from "@komune/g2-themes";
import { StorybookCanvas } from "@komune/g2-storybook-documentation";

import "./default.css";

const defaultTheme = {
  name: "default",
  colors: {
    primary: "#EDBA27",
    secondary: "#353945",
    tertiary: "#e0e0e0",
    error: "#E44258",
    success: "#00CA72",
    warning: "#FF9900",
    info: "#3C78D8",
  },
};

export const parameters = {
  options: {
    storySort: {
      order: [
        "Getting started",
        "SSM-TX",
        ["General", "Configuration", "SSM", "Session"],
        "SSM-CHAINCODE",
        "SSM-COUCHDB",
        ["General", "Configuration", "Model", "Query functions"],
      ]
    }
  },
  docs: {
    container: StorybookCanvas
  }
};

export const withThemeProvider = (Story) => {
  return (
    <ThemeContextProvider theme={defaultTheme}>{Story()}</ThemeContextProvider>
  );
};

export const decorators = [withThemeProvider];
