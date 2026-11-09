import { Contents } from '../component/Contents.tsx'
import { Footer } from '../component/Footer.tsx'
import { Header } from '../component/Header.tsx'
import { SearchNavigation } from '../component/SearchNavigation.tsx'
import { topLevelPages } from '../model/pageApi.ts'
import { useListOfPages } from '../model/useListOfPages.ts'
import './MainPage.css'

/** Main page layout from fe/DESIGN.md: Header, Search and Navigation, Contents, Footer, stacked vertically. */
export function MainPage() {
  const pages = useListOfPages()
  return (
    <div className="main-page">
      <Header />
      <SearchNavigation pages={topLevelPages(pages)} />
      <Contents />
      <Footer />
    </div>
  )
}
