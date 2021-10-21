export function hsvToRgb(hue: number, saturation: number, value: number): [number, number, number] {
    hue = hue % 360;

    const chroma = value * saturation;
    const huePart = hue / 60;

    const x = chroma * (1 - Math.abs(huePart % 2 - 1));
    const m = value - chroma;

    let rawRgb: [number, number, number];
    if (huePart < 1) {
        rawRgb = [chroma, x, 0];
    } else if (huePart < 2) {
        rawRgb = [x, chroma, 0];
    } else if (huePart < 3) {
        rawRgb = [0, chroma, x];
    } else if (huePart < 4) {
        rawRgb = [0, x, chroma];
    } else if (huePart < 5) {
        rawRgb = [x, 0, chroma];
    } else if (huePart < 6) {
        rawRgb = [chroma, 0, x];
    } else {
        return [0, 0, 0];
    }

    return [
        Math.round((rawRgb[0] + m) * 255),
        Math.round((rawRgb[1] + m) * 255),
        Math.round((rawRgb[2] + m) * 255)
    ];
}

export function rgbToHex(red: number, green: number, blue: number, alpha: number = 1): string {
    const convert = (it: number) => {
        const hex = it.toString(16);
        return hex.length >= 2 ? hex : `0${hex}`;
    };
    return `#${convert(red)}${convert(green)}${convert(blue)}${convert(alpha)}`;
}

export function generatePalette(count: number): [number, number, number][] {
    const result: [number, number, number][] = [];

    const huePadding = 10;
    for (let i = 0; i < count; i++) {
        const rgb = hsvToRgb((360 - huePadding * 2) / (i + 1) + huePadding, 0.4, 1.0);
        result.push(rgb);
    }

    return result;
}
