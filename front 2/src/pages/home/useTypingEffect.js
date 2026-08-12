import { useState, useEffect } from "react";

export default function useTypingEffect(text, speed = 100) {
    const [displayed, setDisplayed] = useState("");

    useEffect(() => {
        if (!text) return; // safety check

        let index = 0;
        setDisplayed(""); // reset whenever text changes

        const interval = setInterval(() => {
            if (index <= text.length) {
                // ✅ slice handles Unicode correctly (multi-byte characters)
                setDisplayed(text.slice(0, index));
                index++;
            } else {
                clearInterval(interval);
            }
        }, speed);

        return () => clearInterval(interval);
    }, [text, speed]);

    return displayed;
}
