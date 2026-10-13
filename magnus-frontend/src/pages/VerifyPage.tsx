import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useMutation } from "@tanstack/react-query"
import { verificationService } from "@/api/services"
import { useToast } from "@/hooks/use-toast"
import { ShieldCheck, Search, FileSearch, CheckCircle2, XCircle, Info, Calendar, Hash } from "lucide-react"
import type { VerificationResultDTO, DocumentDTO } from "@/types"

export const VerifyPage: React.FC = () => {
  const { toast } = useToast();
  const [docId, setDocId] = React.useState("");
  const [file, setFile] = React.useState<File | null>(null);
  const [verifyResult, setVerifyResult] = React.useState<VerificationResultDTO | null>(null);
  const [docDetails, setDocDetails] = React.useState<DocumentDTO | null>(null);

  const verifyFileMutation = useMutation({
    mutationFn: (file: File) => verificationService.verifyByFile(file),
    onSuccess: (data) => {
      setVerifyResult(data);
      if (data.authentic) {
        toast({ title: "Verification Passed", description: "This document is authentic." });
      } else {
        toast({ variant: "destructive", title: "Verification Failed", description: data.message });
      }
    },
  });

  const getDetailsMutation = useMutation({
    mutationFn: (id: string) => verificationService.getById(id),
    onSuccess: (data) => {
      setDocDetails(data);
      setVerifyResult({ authentic: true, status: "AUTHENTIC", message: "Found in system" });
    },
    onError: () => {
      setDocDetails(null);
      setVerifyResult({ authentic: false, status: "NOT_FOUND", message: "Document ID not found in our records." });
      toast({ variant: "destructive", title: "Not Found", description: "Could not find a document with that ID." });
    },
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setVerifyResult(null);
      setDocDetails(null);
    }
  };

  const handleVerifyFile = () => {
    if (file) verifyFileMutation.mutate(file);
  };

  const handleVerifyId = () => {
    if (docId) getDetailsMutation.mutate(docId);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="text-center space-y-2 mb-8">
        <h2 className="text-3xl font-bold tracking-tight">Authenticity Verification</h2>
        <p className="text-muted-foreground">Verify the integrity and origin of academic documents</p>
      </div>

      <Tabs defaultValue="upload" className="w-full">
        <TabsList className="grid w-full grid-cols-2 h-12 bg-background/50 border border-border">
          <TabsTrigger value="upload" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
            <FileSearch className="w-4 h-4 mr-2" />
            Verify by File
          </TabsTrigger>
          <TabsTrigger value="id" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
            <Search className="w-4 h-4 mr-2" />
            Verify by ID
          </TabsTrigger>
        </TabsList>

        <TabsContent value="upload" className="mt-6 space-y-6">
          <Card className="bg-card border-border shadow-xl">
            <CardHeader>
              <CardTitle>File Authenticity Check</CardTitle>
              <CardDescription>Upload a document to verify its cryptographic signature</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
               <div 
                className={`border-2 border-dashed rounded-xl p-10 text-center transition-all ${
                  file ? "border-primary bg-primary/5" : "border-border hover:border-primary/50"
                }`}
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                    setFile(e.dataTransfer.files[0]);
                    setVerifyResult(null);
                  }
                }}
              >
                <input type="file" id="verify-upload" className="hidden" onChange={handleFileChange} />
                <label htmlFor="verify-upload" className="cursor-pointer space-y-3 block">
                  <div className="mx-auto w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center">
                    <ShieldCheck className={`w-6 h-6 ${file ? "text-primary" : "text-muted-foreground"}`} />
                  </div>
                  <p className="font-medium">{file ? file.name : "Drop document here to verify"}</p>
                </label>
              </div>
              <Button 
                className="w-full" 
                onClick={handleVerifyFile} 
                disabled={verifyFileMutation.isPending || !file}
              >
                {verifyFileMutation.isPending ? "Verifying..." : "Run Authenticity Check"}
              </Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="id" className="mt-6 space-y-6">
          <Card className="bg-card border-border shadow-xl">
            <CardHeader>
              <CardTitle>ID Lookup</CardTitle>
              <CardDescription>Enter the document UUID to fetch its metadata</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2">
                <Input 
                  placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000" 
                  value={docId}
                  onChange={(e) => setDocId(e.target.value)}
                  className="bg-background/50"
                />
                <Button onClick={handleVerifyId} disabled={getDetailsMutation.isPending || !docId}>
                   {getDetailsMutation.isPending ? "Searching..." : "Lookup"}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {verifyResult && (
        <div className="animate-in fade-in zoom-in-95 duration-500">
           {verifyResult.authentic ? (
             <Card className="border-green-500/50 bg-green-500/5">
                <CardHeader className="flex flex-row items-center gap-4">
                  <div className="p-2 rounded-full bg-green-500/20">
                    <CheckCircle2 className="w-8 h-8 text-green-500" />
                  </div>
                  <div>
                    <CardTitle className="text-green-500 text-xl">AUTHENTIC DOCUMENT</CardTitle>
                    <CardDescription className="text-green-500/70">Cryptographic match found in system</CardDescription>
                  </div>
                </CardHeader>
                <CardContent className="grid gap-4 md:grid-cols-2">
                   {docDetails && (
                     <>
                        <div className="flex items-start gap-3 p-3 rounded-lg bg-background/40">
                          <Calendar className="w-5 h-5 text-primary shrink-0 mt-0.5" />
                          <div>
                            <span className="text-[10px] uppercase font-bold text-muted-foreground">Issue Date</span>
                            <p className="text-sm font-medium">{new Date(docDetails.issueDate).toLocaleString()}</p>
                          </div>
                        </div>
                        <div className="flex items-start gap-3 p-3 rounded-lg bg-background/40">
                          <Hash className="w-5 h-5 text-primary shrink-0 mt-0.5" />
                          <div>
                            <span className="text-[10px] uppercase font-bold text-muted-foreground">Digital Signature</span>
                            <p className="text-sm font-mono truncate w-48">{docDetails.hashValue}</p>
                          </div>
                        </div>
                        <div className="flex items-start gap-3 p-3 rounded-lg bg-background/40 col-span-2">
                          <Info className="w-5 h-5 text-primary shrink-0 mt-0.5" />
                          <div>
                            <span className="text-[10px] uppercase font-bold text-muted-foreground">Verification Message</span>
                            <p className="text-sm">{verifyResult.message}</p>
                          </div>
                        </div>
                     </>
                   )}
                </CardContent>
             </Card>
           ) : (
             <Card className="border-red-500/50 bg-red-500/5">
                <CardHeader className="flex flex-row items-center gap-4">
                   <div className="p-2 rounded-full bg-red-500/20">
                    <XCircle className="w-8 h-8 text-red-500" />
                  </div>
                  <div>
                    <CardTitle className="text-red-500 text-xl">VERIFICATION FAILED</CardTitle>
                    <CardDescription className="text-red-500/70">No matching record or modified content</CardDescription>
                  </div>
                </CardHeader>
                <CardContent>
                  <p className="text-sm font-medium">{verifyResult.message}</p>
                </CardContent>
             </Card>
           )}
        </div>
      )}
    </div>
  )
}
