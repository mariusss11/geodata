import { useState, useEffect } from "react";
import { fetchRecentMaps, useCreateMap, useDeleteMap } from "@/utils/map-service";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { insertMapSchema } from "@shared/schema";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Plus, Trash2 } from "lucide-react";

export default function ManageMaps() {
  const { fetchMaps } = fetchMaps();
  const { createMap } = useCreateMap();
  const { deleteMap } = useDeleteMap();

  const [maps, setMaps] = useState<any[]>([]);
  const [isOpen, setIsOpen] = useState(false);

  // Form setup
  const formSchema = insertMapSchema.omit({ id: true, status: true }).extend({
    lat: z.coerce.number(),
    lng: z.coerce.number(),
  });

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: { name: "", description: "", lat: 0, lng: 0 },
  });

  // Load maps
  useEffect(() => {
    loadMaps();
  }, []);

  async function loadMaps() {
    try {
      const data = await fetchMaps();
      setMaps(data);
    } catch (err) {
      console.error("Failed to fetch maps:", err);
    }
  }

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      await createMap({
        ...values,
        availabilityStatus: "available",
      });
      setIsOpen(false);
      form.reset();
      await loadMaps();
    } catch (err) {
      console.error("Failed to create map:", err);
    }
  }

  async function handleDelete(id: string) {
    try {
      await deleteMap(id);
      await loadMaps();
    } catch (err) {
      console.error("Failed to delete map:", err);
    }
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold font-display tracking-tight text-foreground">Manage Registry</h2>
          <p className="text-muted-foreground">Add, update, or remove maps from the system.</p>
        </div>

        {/* Add Map Dialog */}
        <Dialog open={isOpen} onOpenChange={setIsOpen}>
          <DialogTrigger asChild>
            <Button className="bg-primary hover:bg-primary/90">
              <Plus className="w-4 h-4 mr-2" /> Add New Map
            </Button>
          </DialogTrigger>

          <DialogContent>
            <DialogHeader>
              <DialogTitle>Register New Map</DialogTitle>
              <DialogDescription>Enter the details of the geological data.</DialogDescription>
            </DialogHeader>

            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="name"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Name</FormLabel>
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="description"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Description</FormLabel>
                      <FormControl>
                        <Textarea {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <div className="grid grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="lat"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Latitude</FormLabel>
                        <FormControl>
                          <Input type="number" step="any" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="lng"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Longitude</FormLabel>
                        <FormControl>
                          <Input type="number" step="any" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                <Button type="submit" className="w-full">
                  Create Map
                </Button>
              </form>
            </Form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Maps Table */}
      <div className="bg-card rounded-xl border shadow-sm overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Title</TableHead>
              <TableHead>Location</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {maps?.map((map) => (
              <TableRow key={map.id}>
                <TableCell className="font-medium">{map.title}</TableCell>
                <TableCell className="text-muted-foreground text-xs font-mono">
                  {map.locationData.lat.toFixed(2)}, {map.locationData.lng.toFixed(2)}
                </TableCell>
                <TableCell>
                  <Badge variant={map.status === "available" ? "default" : "secondary"}>
                    {map.status}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="icon" onClick={() => handleDelete(map.id)}>
                    <Trash2 className="w-4 h-4 text-destructive" />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
