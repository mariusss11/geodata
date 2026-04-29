import { fetchMapsStats, MapStats } from "@/utils/map-service";
import { ChartsSection } from "@/components/charts-section";
import { Map, ArrowUpRight, Users, Clock } from "lucide-react";
import { useEffect, useState } from "react";
import { time } from "console";

function StatCard({ title, value, icon: Icon, color }: any) {
  return (
    <div className="bg-card p-6 rounded-2xl border shadow-sm flex items-center justify-between hover-lift">
      <div>
        <p className="text-sm font-medium text-muted-foreground mb-1">{title}</p>
        <h3 className="text-3xl font-bold font-display text-foreground">{value}</h3>
      </div>
      <div className={`p-4 rounded-xl bg-opacity-10 ${color.bg}`}>
        <Icon className={`w-6 h-6 ${color.text}`} />
      </div>
    </div>
  );
}

export default function Dashboard() {

  const [stats, setStats] = useState<MapStats>();
  const [error, setError] = useState('');

  useEffect(() => {
    async function loadStats() {
      try {
        const data = await Promise.race([
          fetchMapsStats(),
          new Promise<never>((_, reject) =>
            setTimeout(() => reject(new Error("Request timed out")), 10000)
          ),
        ]);

        setStats(data);
      } catch (err) {
        console.error("Failed to fetch map stats:", err);
        setError(
          "There was an error displaying the dashboard. The server might be down. Please try again later."
        );
      }
    }

    loadStats();
  }, []);

  if (error) return <div className="p-8 text-red-500">{error}</div>;
  if (!stats) return <div className="p-8">Loading dashboard...</div>;

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-3xl font-bold font-display tracking-tight text-foreground">Dashboard</h2>
        <p className="text-muted-foreground">Overview of the registry system.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <StatCard
          title="Total Maps"
          value={stats.totalMaps}
          icon={Map}
          color={{ bg: "bg-blue-100", text: "text-blue-600" }}
        />
        <StatCard
          title="Currently Borrowed"
          value={stats.borrowedMaps}
          icon={Clock}
          color={{ bg: "bg-indigo-100", text: "text-indigo-600" }}
        />
        <StatCard
          title="Active Users"
          value="999"
          icon={Users}
          color={{ bg: "bg-emerald-100", text: "text-emerald-600" }}
        />
      </div>

      <ChartsSection stats={stats} />

      <div className="bg-gradient-to-r from-slate-900 to-slate-800 rounded-2xl p-8 text-white relative overflow-hidden">
        <div className="relative z-10 max-w-lg">
          <h3 className="text-2xl font-bold font-display mb-2">Explore the Collection</h3>
          <p className="text-slate-300 mb-6">Browse our complete database of high-resolution topographical maps.</p>
          <a href="/maps" className="inline-flex items-center gap-2 bg-white text-slate-900 px-6 py-3 rounded-lg font-bold hover:bg-slate-100 transition-colors">
            Browse Registry <ArrowUpRight className="w-4 h-4" />
          </a>
        </div>
        <div className="absolute right-0 top-0 h-full w-1/3 bg-gradient-to-l from-primary/20 to-transparent" />
      </div>
    </div>
  );
}
