import { MapStats } from '@/utils/map-service';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts';


export function ChartsSection({ stats }: { stats: MapStats }) {
  const pieData = [
    { name: 'Available', value: stats.availableMaps, color: '#10b981' }, // emerald-500
    { name: 'Borrowed', value: stats.borrowedMaps, color: '#6366f1' },  // indigo-500
  ];

  const barData = [
    { name: 'Mon', usage: 4 },
    { name: 'Tue', usage: 7 },
    { name: 'Wed', usage: 5 },
    { name: 'Thu', usage: 12 },
    { name: 'Fri', usage: 9 },
    { name: 'Sat', usage: 3 },
    { name: 'Sun', usage: 2 },
  ];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      {/* Usage Bar Chart */}
      <div className="bg-card p-6 rounded-2xl border shadow-sm">
        <div className="mb-6">
          <h3 className="text-lg font-bold font-display text-foreground">Weekly Activity</h3>
          <p className="text-sm text-muted-foreground">Map borrowing frequency</p>
        </div>
        <div className="h-[250px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={barData}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
              <XAxis
                dataKey="name"
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#64748b', fontSize: 12 }}
                dy={10}
              />
              <YAxis
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#64748b', fontSize: 12 }}
              />
              <Tooltip
                cursor={{ fill: '#f8fafc' }}
                contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
              />
              <Bar
                dataKey="usage"
                fill="var(--primary)"
                radius={[4, 4, 0, 0]}
                barSize={30}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Availability Pie Chart */}
      <div className="bg-card p-6 rounded-2xl border shadow-sm">
        <div className="mb-6">
          <h3 className="text-lg font-bold font-display text-foreground">Inventory Status</h3>
          <p className="text-sm text-muted-foreground">Current availability distribution</p>
        </div>
        <div className="h-[250px] w-full flex items-center justify-center">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={80}
                paddingAngle={5}
                dataKey="value"
              >
                {pieData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} strokeWidth={0} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
        <div className="flex justify-center gap-6 mt-4">
          {pieData.map((item) => (
            <div key={item.name} className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }} />
              <span className="text-sm font-medium text-slate-600">{item.name}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
