import { useState } from 'react';
import { NativeSpeech } from '../plugins/NativeSpeech';
import { Clipboard } from "@capacitor/clipboard";


export default function Transcrition() {
   const [texto, setTexto] = useState("");
   const [ouvindo, setOuvindo] = useState(false);

   async function transcrever() {

      try {

         setOuvindo(true);

         const result = await NativeSpeech.listen();

         const textoTranscrito = result.text;

         // Exibe na tela
         setTexto(textoTranscrito);

         // Copia para o clipboard
         await Clipboard.write({
            string: textoTranscrito
         });

         console.log(
            "Texto transcrito:",
            textoTranscrito
         );

         console.log(
            "Texto copiado para o clipboard"
         );

      } catch (error) {

         console.error(
            "Erro na transcrição:",
            error
         );

      } finally {

         setOuvindo(false);

      }
   }

   return (
      <main>

         <h1>Atena</h1>

         <button
            onClick={transcrever}
            disabled={ouvindo}
         >
            {ouvindo
               ? "🎤 Ouvindo..."
               : "🎤 Transcrever"
            }
         </button>

         <div>
            <h2>Transcrição</h2>

            <p>
               {texto || "Nenhuma transcrição ainda."}
            </p>
         </div>

      </main>
   );
}