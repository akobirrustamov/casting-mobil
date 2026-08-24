import React from 'react';
// import './loader.css'; // если хотите — можно вынести стили отдельно

export default function Loader({ label = 'Loading…' }) {
    return (
        <div style={{height:"100vh"}} className="grid place-items-center gap-2.5 py-12 text-[#8a98ac]" role="status" aria-live="polite">
            <div className="inline-flex gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-[#4da3ff] opacity-80 animate-bbl [animation-duration:1s] [&:nth-child(2)]:[animation-delay:0.12s] [&:nth-child(3)]:[animation-delay:0.24s]" />
                <div className="w-2.5 h-2.5 rounded-full bg-[#4da3ff] opacity-80 animate-bbl [animation-duration:1s] [&:nth-child(2)]:[animation-delay:0.12s] [&:nth-child(3)]:[animation-delay:0.24s]" />
                <div className="w-2.5 h-2.5 rounded-full bg-[#4da3ff] opacity-80 animate-bbl [animation-duration:1s] [&:nth-child(2)]:[animation-delay:0.12s] [&:nth-child(3)]:[animation-delay:0.24s]" />
            </div>
            <div className="text-[13px] text-[#c3cfde]">{label}</div>
        </div>
    );
}
