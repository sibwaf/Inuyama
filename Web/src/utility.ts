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

export function rgbToHex(rgb: [number, number, number], alpha: number = 255): string {
    const convert = (it: number) => {
        const hex = it.toString(16);
        return hex.length >= 2 ? hex : `0${hex}`;
    };
    return `#${convert(rgb[0])}${convert(rgb[1])}${convert(rgb[2])}${convert(alpha)}`;
}

export function generatePalette(count: number): [number, number, number][] {
    const result: [number, number, number][] = [];

    for (let i = 0; i < count; i++) {
        const rgb = hsvToRgb(360 / count * i, 0.4, 1.0);
        result.push(rgb);
    }

    return result;
}
