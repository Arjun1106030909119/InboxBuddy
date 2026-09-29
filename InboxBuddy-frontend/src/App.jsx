
import { CircularProgress, Container, Typography } from '@mui/material'
import { Box, TextField, FormControl, InputLabel, Select, MenuItem } from '@mui/material'
import Button from "@mui/material/Button";
import SendIcon from "@mui/icons-material/Send";
import axios from 'axios'
import { useState } from 'react' 
import './App.css'
import logo from './assets/file.svg'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/email/generate';


function App() {

  const [emailContent, setEmailContent] = useState("");
  const [tone, setTone] = useState("");
  const [generateReply, setGenerateReply] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setLoading(true);
    try {
      const response = await axios.post(API_URL, {
        emailContent, tone
      });
      setGenerateReply(typeof response.data === 'string' ? 
        response.data : JSON.stringify(response.data)
      );
    } catch (error) {
      console.error("Error generating reply:", error);
      setGenerateReply("An error occurred while generating the reply. Please try again.");
    }finally{
      setLoading(false);
    }
    
  };
  return (
    <Container maxWidth="md" sx={{py:4}}>

      <Typography variant="h4" component="h1" gutterBottom>
        <img src={logo} alt="Inbox Buddy Logo" width="150" height="100"/>
      </Typography>

      <Box sx={{mx:3}}>
        <TextField 
        fullWidth
        multiline
        rows={6}
        variant="outlined"
        label="Original Email Content:"
        value={emailContent || ""}
        onChange={(e) => setEmailContent(e.target.value)}
        sx={{mb:2}}/>

        <FormControl fullWidth>
            <InputLabel >Tone (Optional)</InputLabel>
            <Select
              
              value={tone || ""}
              label="Tone (Optional)"
              onChange={(e) => setTone(e.target.value)}
            >
              <MenuItem value="">None</MenuItem>
              <MenuItem value="professional">Professional</MenuItem>
              <MenuItem value="casual">Casual</MenuItem>
              <MenuItem value="friendly">Friendly</MenuItem>
            </Select>
          </FormControl>

          <Button variant="contained" 
          onClick={handleSubmit}
          disabled={!emailContent || loading } endIcon={<SendIcon />} sx={{mb:4, mt:2}}>

              {loading ? <CircularProgress size={24}/>: "Generate Reply"}
            </Button>
      </Box>

      <Box sx={{mx:3}}>
        <TextField 
        fullWidth
        multiline
        rows={6}
        variant="outlined"
        value={generateReply || ""}
        inputProps={{ readOnly: true }}
        sx={{ mb:2 }}/>

        <Button 
        variant='outlined' sx={{ mb:2 }}
        onClick={()=> navigator.clipboard.writeText(generateReply)}>
          Copy to Clipboard
        </Button>
      </Box>
    </Container>
  )
}

export default App
