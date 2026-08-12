import React from 'react';
// import './loader.css'; // если хотите — можно вынести стили отдельно

export default function Loader({ label = 'Loading…' }) {
    return (
        <div style={{height:"100vh"}} className="loader-wrap" role="status" aria-live="polite">
            <div className="spinner">
                <div className="spinner__circle" />
                <div className="spinner__circle" />
                <div className="spinner__circle" />
            </div>
            <div className="loader-text">{label}</div>
        </div>
    );
}
