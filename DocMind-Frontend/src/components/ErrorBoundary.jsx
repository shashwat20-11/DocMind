import React, { Component } from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';

export class ErrorBoundary extends Component {
    state = {
        hasError: false,
        error: null,
    };

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        console.error('Uncaught error in component tree:', error, errorInfo);
    }

    handleReset = () => {
        this.setState({ hasError: false, error: null });
        window.location.reload();
    };

    handleGoHome = () => {
        this.setState({ hasError: false, error: null });
        window.location.href = '/';
    };

    render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4 text-slate-100">
                    <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl text-center space-y-5">
                        <div className="w-14 h-14 bg-red-500/10 border border-red-500/20 text-red-400 rounded-2xl flex items-center justify-center mx-auto shadow-inner">
                            <AlertTriangle className="w-7 h-7" />
                        </div>

                        <div className="space-y-2">
                            <h2 className="text-xl font-bold text-white tracking-tight">Something went wrong</h2>
                            <p className="text-sm text-slate-400 leading-relaxed">
                                An unexpected error occurred in the application. You can try refreshing the page or returning home.
                            </p>
                        </div>

                        {this.state.error?.message && (
                            <div className="bg-slate-950/80 border border-slate-800/80 rounded-xl p-3 text-left">
                                <p className="text-xs font-mono text-red-400/90 break-words line-clamp-3">
                                    {this.state.error.message}
                                </p>
                            </div>
                        )}

                        <div className="flex gap-3 justify-center pt-2">
                            <button
                                onClick={this.handleReset}
                                className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 active:scale-95 text-white text-sm font-medium rounded-xl transition shadow-lg shadow-indigo-600/25 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                            >
                                <RefreshCw className="w-4 h-4" />
                                Reload Application
                            </button>
                            <button
                                onClick={this.handleGoHome}
                                className="flex items-center gap-2 px-4 py-2.5 bg-slate-800 hover:bg-slate-700 active:scale-95 text-slate-300 hover:text-white text-sm font-medium rounded-xl transition border border-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-500"
                            >
                                <Home className="w-4 h-4" />
                                Home
                            </button>
                        </div>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;