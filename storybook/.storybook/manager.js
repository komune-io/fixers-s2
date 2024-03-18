import { addons } from '@storybook/addons';
import {create} from "@storybook/theming";
import logo from "../assets/logo.png";

addons.setConfig({
    theme: create({
        base: 'light',
        brandTitle: 'Fixers S2',
        brandUrl: "https://komune-io.github.io/fixers-s2/",
        brandImage: logo,
    }),
    showToolbar: false
});
