import { useState } from "react";
import { Clipboard } from "@capacitor/clipboard";
import { NativeSpeech } from "./plugins/nativeSpeech";

function App() {

    const [texto, setTexto] = useState("");
    const [ouvindo, setOuvindo] = useState(false);

    async function transcrever() {

        try {

            setOuvindo(true);

            console.log("Chamando Android...");

            const result =
                await NativeSpeech.listen();

            console.log(
                "Resultado:",
                result
            );

            const textoTranscrito =
                result.text;

            setTexto(
                textoTranscrito
            );

            await Clipboard.write({
                string: textoTranscrito
            });

            console.log(
                "Copiado para clipboard"
            );

        } catch (error) {

            console.error(
                "Erro:",
                error
            );

        } finally {

            setOuvindo(false);

        }
    }

    return (
        <div>

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

            <textarea
                value={texto}
                onChange={(e) =>
                    setTexto(e.target.value)
                }
                placeholder="A transcrição aparecerá aqui..."
                rows={5}
            />

        </div>
    );
}

export default App;